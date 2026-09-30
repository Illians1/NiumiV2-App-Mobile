#!/usr/bin/env bash
#
# Protocole de validation du blocage d'applications (SPEC_ANDROID §12.2, §19.2, §20).
#
# Les deux vérifications de §19.2 — « overlay d'accessibilité sur application factice » et
# « retour à l'accueil après détection d'un package bloqué » — ne sont pas réalisables par un
# test instrumenté : toute instrumentation de `com.niumi.app` fait passer
# `accessibility_enabled` à 0 et débranche le service, qui ne se relie pas de lui-même
# (mesuré à l'étape 5, voir docs/android/implementation-reports/ETAPE-05.md). Ce script est
# leur substitut : il déroule le protocole sur un appareil réel et vérifie chaque essai par
# `dumpsys`, plutôt que de s'en remettre à l'œil de l'opérateur.
#
# Ce qu'il ne vérifie PAS, faute d'accès au contenu de la fenêtre : le texte exact de l'overlay
# imposé par §12.2. Il constate la présence d'une fenêtre `TYPE_ACCESSIBILITY_OVERLAY`, pas ce
# qu'elle affiche. Ce contrôle-là reste visuel.
#
# Préconditions, à préparer avant de lancer (le script les vérifie et s'arrête sinon) :
#   1. un seul appareil branché, débogage USB activé ;
#   2. Niumi installé (`./gradlew :app:installDebug`) ;
#   3. le service d'accessibilité Niumi activé à la main — §12.3 interdit de simuler un
#      consentement, et une réinstallation le désactive systématiquement ;
#   5. une session Niumi armée, avec le package à bloquer dans la sélection d'applications.
#      Le blocage n'existe pas en dehors d'une session (§12.2) : depuis l'étape 21 et la
#      suppression de la route POC, il n'y a plus d'autre moyen de l'armer que le parcours réel ;
#   4. Niumi exempté des restrictions d'énergie du constructeur. Sans cela, la surcouche gèle
#      le process en arrière-plan et le blocage devient silencieusement inopérant (§13). Cette
#      précondition n'est pas détectable de façon fiable : `isIgnoringBatteryOptimizations()`
#      renvoie `false` sur HyperOS y compris quand le réglage OEM est correct.
#
# Ne jamais lancer `adb shell am force-stop com.niumi.app` pendant ce protocole : Android
# retirerait le service de la liste des services activés et tous les essais échoueraient.
#
# Mode « début différé » (Lot 6, étape 25), option --deferred : ajoute un essai préalable qui
# couvre la ligne §20 « blocage différé, application bloquée déjà au premier plan à l'heure de
# début » et le paragraphe « Application déjà ouverte » de §12.4. La précondition 5 devient alors :
# une session armée avec « À partir de » réglé dans 5 à 10 minutes et l'heure de réveil bien
# au-delà. L'essai ouvre l'application AVANT l'instant de début, la laisse au premier plan, et
# mesure le retour à l'accueil. Les six essais ordinaires suivent : une fois l'instant passé, la
# session est exactement dans l'état qu'ils attendent. L'inverse n'est pas vrai, d'où un mode
# plutôt qu'un septième essai.
#
# Usage : tools/validate_blocking.sh [--deferred] <package.bloque> [package.non.bloque]
# Exemple : tools/validate_blocking.sh com.miui.calculator com.android.deskclock
# Exemple : tools/validate_blocking.sh --deferred com.miui.calculator

set -u

readonly NIUMI_PACKAGE="com.niumi.app"
readonly SERVICE_COMPONENT="${NIUMI_PACKAGE}/com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService"
readonly BLOCK_TIMEOUT_S=5
readonly OVERLAY_TIMEOUT_S=4
readonly BLOCKING_START_TAG="${NIUMI_PACKAGE}/com.niumi.system.blocking.BlockingStartReceiver"
# Marge minimale restante à l'issue des préconditions : en deçà, l'essai n'a pas le temps de se
# préparer avant l'instant de début.
readonly DEFERRED_MIN_MARGIN_S=120
# L'application est ouverte ce nombre de secondes avant l'instant, pour prouver qu'aucun blocage
# n'a lieu avant l'heure autant que pour être au premier plan quand elle arrive.
readonly DEFERRED_LEAD_S=60
# §20 : « retard mesuré inférieur à 1 minute ».
readonly DEFERRED_GRACE_S=60

DEFERRED=0
if [ "${1:-}" = "--deferred" ]; then
    DEFERRED=1
    shift
fi

BLOCKED_PACKAGE="${1:-}"
ALLOWED_PACKAGE="${2:-}"
stayon_set=0
DEFERRED_STARTS_AT_MS=0
failures=0

log_pass() { printf '  \033[32mOK\033[0m   %s\n' "$1"; }
log_fail() { printf '  \033[31mÉCHEC\033[0m %s\n' "$1"; failures=$((failures + 1)); }
log_info() { printf '       %s\n' "$1"; }
log_step() { printf '\n== %s\n' "$1"; }

fatal() {
    printf '\033[31mArrêt : %s\033[0m\n' "$1" >&2
    exit 2
}

top_package() {
    adb shell dumpsys activity activities 2>/dev/null |
        grep -m1 -i 'topResumedActivity' |
        sed 's/.*u0 //; s|/.*||' |
        tr -d '\r'
}

overlay_window_count() {
    adb shell dumpsys window windows 2>/dev/null | grep -ci 'ty=ACCESSIBILITY_OVERLAY'
}

# Le processus de l'application existe-t-il ? C'est la preuve que le lancement a bien eu lieu,
# indépendamment de l'échantillonnage du premier plan : un blocage rapide renvoie à l'accueil
# en moins d'une seconde, si bien qu'un sondage périodique peut ne jamais voir l'application
# au premier plan alors qu'elle a bel et bien démarré. Le retour à l'accueil ne tue pas le
# processus, contrairement à `am force-stop` qui précède chaque essai.
process_exists() {
    [ -n "$(adb shell pidof "$1" 2>/dev/null | tr -d '\r')" ]
}

# Attend que le package au premier plan ne soit plus celui attendu bloqué, ce qui matérialise
# le GLOBAL_ACTION_HOME. Renvoie 0 si le retour a eu lieu dans le délai imparti.
wait_until_left_foreground() {
    local package="$1" deadline=$((SECONDS + BLOCK_TIMEOUT_S))
    while [ "$SECONDS" -lt "$deadline" ]; do
        [ "$(top_package)" != "$package" ] && return 0
        sleep 1
    done
    return 1
}

# Lance l'application et vérifie le blocage. Distingue trois issues, là où une seule assertion
# les confondrait : lancement impossible (essai non concluant), application restée au premier
# plan (échec réel), retour à l'accueil (succès).
assert_blocked_after_launch() {
    local package="$1" launcher="$2"
    "$launcher" "$package"
    local left_foreground=1
    wait_until_left_foreground "$package" && left_foreground=0

    if ! process_exists "$package"; then
        log_fail "l'application n'a pas démarré — essai non concluant, blocage non éprouvé"
        return
    fi
    if [ "$left_foreground" -eq 0 ]; then
        log_pass "démarrée puis renvoyée à l'accueil (top = $(top_package))"
    else
        log_fail "l'application est restée au premier plan pendant ${BLOCK_TIMEOUT_S} s"
    fi
}

launch_from_launcher() {
    adb shell monkey -p "$1" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
}

# Bloc `dumpsys` de l'alarme de début du blocage, et rien d'autre : de son en-tête `Alarm{...}`
# jusqu'à l'alarme suivante. L'en-tête doit précéder IMMÉDIATEMENT la ligne `tag=` — le nom du
# receveur réapparaît dans les statistiques du dump bien après la délivrance, où un `grep -B`
# ramasserait l'en-tête d'une autre alarme et ferait croire que celle-ci est toujours en attente.
blocking_start_block() {
    adb shell dumpsys alarm 2>/dev/null | tr -d '\r' | awk -v tag="$BLOCKING_START_TAG" '
        inblk { if (index($0, "Alarm{")) exit; print; next }
        hdrprev && index($0, tag) { print hdr; print; inblk = 1; next }
        { hdrprev = index($0, "Alarm{") > 0; if (hdrprev) hdr = $0 }
    '
}

# Instant contractuel de l'alarme de début, en millisecondes, ou rien si elle n'existe pas.
#
# Lu dans l'en-tête `Alarm{... origWhen 1790240960149 ...}`, qui porte l'epoch brut d'une alarme
# RTC. La ligne de détail affiche aussi `origWhen=2026-09-24 11:09:20.149`, mais la convertir
# demande `date -D` sur l'appareil, qui ignore l'heure d'été : mesuré le 2026-09-24, décalage
# d'une heure pile. Aucune mesure ne repose donc sur une date formatée — c'est la confusion entre
# les deux formes qui avait laissé un parsing muet dans `tools/validate_alarm.sh` (voir ETAPE-25.md ;
# corrigé le 2026-09-30 en reprenant cette lecture).
blocking_start_when_ms() {
    local raw
    raw=$(blocking_start_block | head -1 | sed -n 's/.*origWhen \([0-9]\{13\}\) .*/\1/p')
    [ -n "$raw" ] || return 1
    printf '%s\n' "$raw"
}

device_now_ms() {
    adb shell date +%s%3N 2>/dev/null | tr -d '\r'
}

# L'horloge de l'appareil doit être lisible à la milliseconde : c'est elle, et jamais celle du
# poste, qui sert de référence au retard mesuré.
require_device_clock() {
    local now_ms
    now_ms=$(device_now_ms)
    case "$now_ms" in
        ''|*[!0-9]*) fatal "l'horloge de l'appareil est illisible (adb shell date +%s%3N)." ;;
    esac
    [ "${#now_ms}" -eq 13 ] || fatal "horloge de l'appareil sans millisecondes (${now_ms})."
    log_pass "horloge de l'appareil lisible à la milliseconde"
}

require_preconditions() {
    log_step "Préconditions"

    command -v adb >/dev/null 2>&1 || fatal "adb est introuvable dans le PATH."

    local devices
    devices=$(adb devices | grep -cw 'device')
    [ "$devices" -eq 1 ] || fatal "$devices appareil(s) détecté(s) ; il en faut exactement un."
    log_pass "un appareil branché"

    adb shell pm list packages 2>/dev/null | grep -qx "package:${NIUMI_PACKAGE}" ||
        fatal "${NIUMI_PACKAGE} n'est pas installé (./gradlew :app:installDebug)."
    log_pass "Niumi installé"

    local enabled services
    enabled=$(adb shell settings get secure accessibility_enabled 2>/dev/null | tr -d '\r')
    services=$(adb shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r')
    if [ "$enabled" != "1" ] || [[ "$services" != *"$SERVICE_COMPONENT"* ]]; then
        fatal "service d'accessibilité Niumi inactif. L'activer à la main : Réglages → Accessibilité → Applications téléchargées → Niumi."
    fi
    log_pass "service d'accessibilité activé"

    adb shell dumpsys accessibility 2>/dev/null | grep -q 'label=Niumi' ||
        fatal "le service est activé mais non lié par le système. Le désactiver puis le réactiver."
    log_pass "service lié par le système"

    adb shell pm list packages 2>/dev/null | grep -qx "package:${BLOCKED_PACKAGE}" ||
        fatal "${BLOCKED_PACKAGE} n'est pas installé sur cet appareil."
    log_pass "application cible installée : ${BLOCKED_PACKAGE}"

    log_info "Rappel : l'exemption d'énergie constructeur n'est pas vérifiable ici (§13)."
    log_info "Un échec généralisé des essais doit d'abord faire suspecter ce réglage."
}

require_block_armed() {
    log_step "Armement du blocage"
    log_info "Dans Niumi : armer une session avec ${BLOCKED_PACKAGE} dans la sélection"
    log_info "d'applications (diagnostic, boîtier, applications, heure, activation)."
    printf '       Appuyer sur Entrée une fois la session armée... '
    read -r _
}

# §9.1, seconde dérogation : le début du blocage ne doit JAMAIS atteindre le réglage « prochaine
# alarme » du système, sans quoi l'appareil annoncerait une alarme à une heure où Niumi ne sonnera
# pas. `BlockingStartAlarmVisibilityTest` le prouve en instrumenté sur des alarmes synthétiques ;
# ici, c'est une vraie session armée par le parcours réel.
assert_hidden_from_next_alarm_clock() {
    local starts_at_ms="$1" section
    section=$(adb shell dumpsys alarm 2>/dev/null | tr -d '\r' |
        sed -n '/Next alarm clock information:/,/pending alarms:/p')
    if [ -z "$section" ]; then
        log_info "section « Next alarm clock information » absente du dump, contrôle §9.1 non concluant."
        return
    fi
    if printf '%s\n' "$section" | grep -q "time:${starts_at_ms}"; then
        log_fail "l'instant de début figure dans « prochaine alarme » — §9.1 violée, revenir à la spec"
    else
        log_pass "l'instant de début n'atteint pas le réglage « prochaine alarme » (§9.1)"
    fi
}

require_deferred_session_armed() {
    log_step "Armement d'une session à blocage différé"
    log_info "Dans Niumi, parcours complet : ${BLOCKED_PACKAGE} dans la sélection d'applications,"
    log_info "heure de réveil dans plus d'une heure, section « Blocage des applications » →"
    log_info "« À partir de » → une heure dans 5 à 10 minutes. Activer."
    log_info "L'écran 7 doit afficher « Réveil programmé · blocage à HH:MM » et « Début du blocage »."
    log_info ""
    log_info "Ensuite, NE PLUS TOUCHER AU TÉLÉPHONE : cet essai prouve un retour à l'accueil sans"
    log_info "changement de fenêtre (§12.4). Toute interaction l'invalide."
    printf '       Appuyer sur Entrée une fois la session armée... '
    read -r _

    local starts_at_ms now_ms margin_s
    starts_at_ms=$(blocking_start_when_ms) ||
        fatal "aucune alarme de début en attente : la session n'est pas différée, ou pas armée."
    log_pass "alarme de début trouvée (origWhen=${starts_at_ms})"

    printf '%s\n' "$(blocking_start_block)" | sed 's/^/       | /'

    if printf '%s\n' "$(blocking_start_block)" | grep -q 'exactAllowReason=policy_permission'; then
        log_pass "exactAllowReason=policy_permission (l'alarme est affranchie des politiques Doze)"
    else
        log_fail "exactAllowReason inattendu — la seconde dérogation de §9.1 repose dessus"
    fi

    assert_hidden_from_next_alarm_clock "$starts_at_ms"

    now_ms=$(device_now_ms)
    margin_s=$(( (starts_at_ms - now_ms) / 1000 ))
    [ "$margin_s" -ge "$DEFERRED_MIN_MARGIN_S" ] ||
        fatal "il ne reste que ${margin_s} s avant l'instant de début ; réarmer une session plus lointaine."
    log_pass "marge avant l'instant de début : ${margin_s} s"

    # L'écran doit rester allumé : §12.4 admet que le dernier package vu peut être périmé écran
    # éteint, et c'est une AUTRE ligne de §20 (« écran éteint depuis 30 minutes »). Cet essai-ci
    # porte sur l'application au premier plan, écran allumé.
    adb shell svc power stayon usb >/dev/null 2>&1 && stayon_set=1
    log_info "écran maintenu allumé pendant l'essai ; il sera rendu au système ensuite."

    DEFERRED_STARTS_AT_MS="$starts_at_ms"
}

# Attend que le package quitte le premier plan et rend l'instant, en millisecondes de l'appareil.
# La boucle tourne SUR l'appareil, en un seul `adb shell` : mesurée depuis le poste, chaque tour
# paierait la traversée USB, qui est du même ordre que le retard à mesurer.
wait_home_return_ms() {
    local package="$1" deadline_ms="$2"
    adb shell "
        while :; do
            now=\$(date +%s%3N)
            if ! dumpsys activity activities | grep -m1 topResumedActivity | grep -q '$package'; then
                echo \"\$now\"
                exit 0
            fi
            if [ \"\$now\" -gt $deadline_ms ]; then
                echo TIMEOUT
                exit 1
            fi
        done
    " 2>/dev/null | tr -d '\r' | tail -1
}

# Essai différé : aucun blocage avant l'instant, retour à l'accueil à l'instant, sans changement
# de fenêtre. §20 (« application bloquée déjà au premier plan à l'heure de début ») et §12.4.
test_deferred_start() {
    log_step "Essai 0 — début différé, application déjà au premier plan"
    local starts_at_ms="$DEFERRED_STARTS_AT_MS" now_ms wait_s changed_ms delay_ms

    now_ms=$(device_now_ms)
    wait_s=$(( (starts_at_ms - now_ms) / 1000 - DEFERRED_LEAD_S ))
    if [ "$wait_s" -gt 0 ]; then
        log_info "attente de ${wait_s} s, jusqu'à ${DEFERRED_LEAD_S} s avant l'instant de début..."
        sleep "$wait_s"
    fi

    # Dernières interactions autorisées : à partir du lancement, plus aucune commande ne touche
    # aux fenêtres, ce qui est la seule preuve possible du « sans changement de fenêtre ».
    adb shell am force-stop "$BLOCKED_PACKAGE" >/dev/null 2>&1
    adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1
    sleep 2
    launch_from_launcher "$BLOCKED_PACKAGE"
    sleep 3

    if ! process_exists "$BLOCKED_PACKAGE"; then
        log_fail "l'application n'a pas démarré — essai non concluant"
        return
    fi
    if [ "$(top_package)" != "$BLOCKED_PACKAGE" ]; then
        log_fail "l'application n'est pas au premier plan avant l'instant de début — essai non concluant"
        return
    fi
    log_pass "application au premier plan avant l'instant de début, aucun retour à l'accueil"
    if [ "$(overlay_window_count)" -eq 0 ]; then
        log_pass "aucun overlay avant l'instant de début"
    else
        log_fail "overlay affiché AVANT l'instant de début — §12.4 interdit tout blocage avant l'heure"
    fi

    changed_ms=$(wait_home_return_ms "$BLOCKED_PACKAGE" $((starts_at_ms + DEFERRED_GRACE_S * 1000)))
    if [ "$changed_ms" = "TIMEOUT" ]; then
        log_fail "application restée au premier plan ${DEFERRED_GRACE_S} s après l'instant de début"
        return
    fi

    delay_ms=$((changed_ms - starts_at_ms))
    if [ "$delay_ms" -lt -1000 ]; then
        log_fail "blocage prématuré de $(( -delay_ms )) ms — §12.4 interdit tout blocage avant l'instant"
    else
        log_pass "retour à l'accueil ${delay_ms} ms après l'instant contractuel (top = $(top_package))"
    fi

    if [ "$(overlay_window_count)" -ge 1 ]; then
        log_pass "fenêtre TYPE_ACCESSIBILITY_OVERLAY présente"
    else
        log_info "overlay non observé : son minuteur de 3 s a pu l'avoir déjà retiré (§12.2)."
    fi

    log_info "À reporter dans QA_MATRIX.md : instant ${starts_at_ms}, observé ${changed_ms}, retard ${delay_ms} ms."
    log_info "Précision bornée par le coût d'un dumpsys activity (~150-300 ms) : le sondage ne peut pas mieux."
    log_info "« Sans changement de fenêtre » : aucune commande n'a touché aux fenêtres depuis le lancement."
}

# Essai 1 : tâche neuve. L'application n'existe pas dans les récents, son activité est donc
# réellement démarrée.
test_fresh_task() {
    log_step "Essai 1 — ouverture depuis le lanceur, tâche neuve"
    adb shell am force-stop "$BLOCKED_PACKAGE" >/dev/null 2>&1
    adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1
    sleep 2
    assert_blocked_after_launch "$BLOCKED_PACKAGE" launch_from_launcher
}

# Essai 2 : tâche existante ramenée au premier plan — le chemin le plus courant (récents, ou
# icône d'une application déjà lancée). Android n'émet alors pas toujours les mêmes événements
# qu'à un démarrage d'activité, d'où un essai distinct.
test_existing_task() {
    log_step "Essai 2 — réouverture depuis le lanceur, tâche existante"
    assert_blocked_after_launch "$BLOCKED_PACKAGE" launch_from_launcher
}

# Essai 3 : intent explicite vers l'activité, équivalent du chemin « depuis une notification »
# (une notification ouvre l'application via un PendingIntent).
test_explicit_intent() {
    log_step "Essai 3 — ouverture par intent explicite (équivalent notification)"
    local component
    component=$(adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "$BLOCKED_PACKAGE" 2>/dev/null | tail -1 | tr -d '\r')
    if [ -z "$component" ]; then
        log_fail "impossible de résoudre l'activité de lancement de ${BLOCKED_PACKAGE}"
        return
    fi
    launch_component() { adb shell am start -n "$component" >/dev/null 2>&1; }
    assert_blocked_after_launch "$BLOCKED_PACKAGE" launch_component
}

# §12.2 : l'overlay est affiché au moment du blocage, puis retiré par le minuteur de 3 s. La
# clause « dès que le package bloqué n'est plus au premier plan » ne s'applique pas, Niumi
# venant lui-même de renvoyer à l'accueil (voir ETAPE-05.md).
test_overlay() {
    log_step "Essai 4 — overlay affiché puis retiré"
    adb shell am force-stop "$BLOCKED_PACKAGE" >/dev/null 2>&1
    adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1
    sleep 2
    launch_from_launcher "$BLOCKED_PACKAGE"
    sleep 1
    if ! process_exists "$BLOCKED_PACKAGE"; then
        log_fail "l'application n'a pas démarré — essai non concluant"
        return
    fi
    if [ "$(overlay_window_count)" -ge 1 ]; then
        log_pass "fenêtre TYPE_ACCESSIBILITY_OVERLAY présente"
        log_info "Contrôle visuel attendu : « <Nom de l'application> reste bloquée jusqu'au scan du boîtier. »"
    else
        log_fail "aucune fenêtre TYPE_ACCESSIBILITY_OVERLAY pendant le blocage"
    fi

    sleep "$OVERLAY_TIMEOUT_S"
    if [ "$(overlay_window_count)" -eq 0 ]; then
        log_pass "overlay retiré après ${OVERLAY_TIMEOUT_S} s"
    else
        log_fail "overlay toujours affiché après ${OVERLAY_TIMEOUT_S} s"
    fi
}

# §12.2 : une application hors de la liste de blocage ne doit subir aucun effet.
test_allowed_application() {
    [ -n "$ALLOWED_PACKAGE" ] || return 0
    log_step "Essai 5 — application non bloquée"
    if ! adb shell pm list packages 2>/dev/null | grep -qx "package:${ALLOWED_PACKAGE}"; then
        log_info "${ALLOWED_PACKAGE} n'est pas installé, essai ignoré."
        return 0
    fi
    adb shell am force-stop "$ALLOWED_PACKAGE" >/dev/null 2>&1
    adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1
    sleep 2
    launch_from_launcher "$ALLOWED_PACKAGE"
    sleep 3
    if [ "$(top_package)" = "$ALLOWED_PACKAGE" ]; then
        log_pass "l'application reste au premier plan"
    else
        log_fail "l'application a été renvoyée à l'accueil alors qu'elle n'est pas bloquée"
    fi
}

# Le gel du process en arrière-plan par la gestion d'énergie du constructeur ne se manifeste
# qu'après un délai : un essai immédiat ne le détecte pas (§13, mesuré à l'étape 5).
test_after_delay() {
    local delay_s="${NIUMI_DELAY_S:-90}"
    log_step "Essai 6 — blocage encore actif après ${delay_s} s en arrière-plan"
    adb shell am force-stop "$BLOCKED_PACKAGE" >/dev/null 2>&1
    adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1
    log_info "attente de ${delay_s} s sans interaction avec Niumi..."
    sleep "$delay_s"
    assert_blocked_after_launch "$BLOCKED_PACKAGE" launch_from_launcher
    log_info "En cas d'échec ici alors que l'essai 1 passait : suspecter l'exemption d'énergie (§13)."
}

main() {
    if [ -z "$BLOCKED_PACKAGE" ]; then
        printf 'Usage : %s [--deferred] <package.bloque> [package.non.bloque]\n' "$0" >&2
        exit 2
    fi

    require_preconditions
    if [ "$DEFERRED" -eq 1 ]; then
        require_device_clock
        require_deferred_session_armed
        test_deferred_start
    else
        require_block_armed
    fi
    test_fresh_task
    test_existing_task
    test_explicit_intent
    test_overlay
    test_allowed_application
    test_after_delay

    log_step "Résultat"
    adb shell am force-stop "$BLOCKED_PACKAGE" >/dev/null 2>&1
    [ "$stayon_set" -eq 1 ] && adb shell svc power stayon false >/dev/null 2>&1
    if [ "$failures" -eq 0 ]; then
        printf '  Tous les essais automatisables sont passés.\n'
        printf '  Restent à vérifier à l'"'"'œil : le texte exact de l'"'"'overlay (§12.2) et le fait\n'
        printf '  qu'"'"'il ne rende pas le téléphone inutilisable.\n'
        printf '  Penser à terminer la session par un scan du boîtier (§3 : aucune autre sortie).\n'
        exit 0
    fi
    printf '  \033[31m%s essai(s) en échec.\033[0m\n' "$failures"
    exit 1
}

main "$@"
