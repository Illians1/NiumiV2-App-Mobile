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
#   4. Niumi exempté des restrictions d'énergie du constructeur. Sans cela, la surcouche gèle
#      le process en arrière-plan et le blocage devient silencieusement inopérant (§13). Cette
#      précondition n'est pas détectable de façon fiable : `isIgnoringBatteryOptimizations()`
#      renvoie `false` sur HyperOS y compris quand le réglage OEM est correct.
#
# Ne jamais lancer `adb shell am force-stop com.niumi.app` pendant ce protocole : Android
# retirerait le service de la liste des services activés et tous les essais échoueraient.
#
# Usage : tools/validate_blocking.sh <package.bloque> [package.non.bloque]
# Exemple : tools/validate_blocking.sh com.miui.calculator com.android.deskclock

set -u

readonly NIUMI_PACKAGE="com.niumi.app"
readonly SERVICE_COMPONENT="${NIUMI_PACKAGE}/com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService"
readonly BLOCK_TIMEOUT_S=5
readonly OVERLAY_TIMEOUT_S=4

BLOCKED_PACKAGE="${1:-}"
ALLOWED_PACKAGE="${2:-}"
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
    log_info "Dans Niumi : POC → « Package à bloquer » = ${BLOCKED_PACKAGE} → « Bloquer »."
    printf '       Appuyer sur Entrée une fois le blocage armé... '
    read -r _
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
        printf 'Usage : %s <package.bloque> [package.non.bloque]\n' "$0" >&2
        exit 2
    fi

    require_preconditions
    require_block_armed
    test_fresh_task
    test_existing_task
    test_explicit_intent
    test_overlay
    test_allowed_application
    test_after_delay

    log_step "Résultat"
    adb shell am force-stop "$BLOCKED_PACKAGE" >/dev/null 2>&1
    if [ "$failures" -eq 0 ]; then
        printf '  Tous les essais automatisables sont passés.\n'
        printf '  Restent à vérifier à l'"'"'œil : le texte exact de l'"'"'overlay (§12.2) et le fait\n'
        printf '  qu'"'"'il ne rende pas le téléphone inutilisable.\n'
        printf '  Penser à retirer le blocage dans le POC.\n'
        exit 0
    fi
    printf '  \033[31m%s essai(s) en échec.\033[0m\n' "$failures"
    exit 1
}

main "$@"
