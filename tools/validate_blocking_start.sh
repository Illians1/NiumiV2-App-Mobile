#!/usr/bin/env bash
#
# Résilience de l'alarme de début du blocage différé (Lot 6, étape 25).
#
# Couvre deux lignes de SPEC_ANDROID §20 que ni la JVM ni l'instrumentation ne savent atteindre :
#   - « blocage différé, redémarrage avant l'heure de début, aucun déverrouillage » : l'alarme
#     doit être reprogrammée depuis Direct Boot, au MÊME instant, avant tout déverrouillage (§9.3) ;
#   - « blocage différé, Doze forcé à l'heure de début » : l'alarme doit être délivrée à l'heure,
#     comme le watchdog de §4.2. C'est cet essai qui décide si la seconde dérogation de §9.1
#     (`setExactAndAllowWhileIdle()` plutôt que `setAlarmClock()`) tient sur cet appareil.
#
# Ce qu'il ne vérifie PAS : que le blocage soit effectivement appliqué aux applications — cela
# demande le service d'accessibilité et se mesure par `tools/validate_blocking.sh --deferred`.
# Ici, la preuve est la programmation de l'alarme et sa délivrance, lues dans `dumpsys alarm`.
#
# Préconditions, à préparer avant de lancer (le script les vérifie et s'arrête sinon) :
#   1. un seul appareil, débogage USB activé ;
#   2. Niumi installé ;
#   3. une session Niumi armée avec un blocage différé encore à venir — au moins 5 min pour
#      l'essai « reboot », au moins 10 min pour l'essai « doze ». Depuis la suppression de la
#      route POC (étape 21), le seul moyen de l'armer est le parcours réel ;
#   4. un code de verrouillage d'écran. Sans lui, le chiffrement par fichier déverrouille
#      l'utilisateur seul au démarrage et « sans déverrouillage » ne veut plus rien dire ;
#   5. permission OEM « Démarrage automatique en arrière-plan » dans son état PAR DÉFAUT, refusé
#      (§20, §4.2). Non vérifiable par adb : à consigner à la main dans QA_MATRIX.md.
#
# LES DEUX ESSAIS N'UTILISENT PAS LE MÊME TRANSPORT. Le Doze profond est inatteignable câble
# branché : `force-idle` s'arrête à INACTIVE, `dumpsys battery unplug` ne trompant que la couche
# batterie et non la politique d'inactivité (mesuré à la porte 0, voir LOT-0.md). L'essai « doze »
# exige donc adb par Wi-Fi, câble retiré — ce que `adb reboot` ne supporte pas. Le script le
# détecte et refuse d'enchaîner aveuglément.
#
# Ne jamais lancer `adb shell am force-stop com.niumi.app` pendant ce protocole : le paquet
# passerait à l'état « stopped », où il ne reçoit plus aucun broadcast — l'alarme de début ne
# serait pas reçue et l'essai mesurerait autre chose.
#
# Usage : tools/validate_blocking_start.sh [reboot|doze|all]     (défaut : all)
# Exemple : tools/validate_blocking_start.sh reboot
# Exemple : ADB="adb -s 192.168.1.20:5555" tools/validate_blocking_start.sh doze

set -u

readonly NIUMI_PACKAGE="com.niumi.app"
readonly BLOCKING_START_TAG="${NIUMI_PACKAGE}/com.niumi.system.blocking.BlockingStartReceiver"
# Le réveil vit dans `:feature:ringing`, pas dans `:core:system` : §20 exige de constater que le
# réveil reste intact après le redémarrage, et viser le mauvais composant le dirait toujours absent.
readonly WAKE_ALARM_TAG="${NIUMI_PACKAGE}/com.niumi.feature.ringing.AlarmReceiver"
readonly BOOT_TIMEOUT_S=180
readonly ALARM_RESTORE_TIMEOUT_S=120
readonly REBOOT_MIN_MARGIN_S=300
readonly DOZE_MIN_MARGIN_S=600
readonly DOZE_STEP_MAX=15
readonly DOZE_POLL_S=2
# §20 : « retard mesuré inférieur à 1 minute ».
readonly DOZE_MAX_DELAY_S=60

# Résolution d'adb avec repli, reprise de validate_alarm.sh et capture_device_state.sh. Déviation
# assumée par rapport à validate_blocking.sh, qui appelle `adb` en dur : ce script-ci redémarre
# l'appareil et change de transport en cours de route, et un adb introuvable à mi-parcours
# laisserait la machine en Doze forcé.
ADB="${ADB:-adb}"
if ! command -v "$ADB" >/dev/null 2>&1; then
    if [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
        ADB="$HOME/Library/Android/sdk/platform-tools/adb"
    fi
fi

MODE="${1:-all}"
doze_forced=0
failures=0

log_pass() { printf '  \033[32mOK\033[0m   %s\n' "$1"; }
log_fail() { printf '  \033[31mÉCHEC\033[0m %s\n' "$1"; failures=$((failures + 1)); }
log_info() { printf '       %s\n' "$1"; }
log_step() { printf '\n== %s\n' "$1"; }

fatal() {
    printf '\033[31mArrêt : %s\033[0m\n' "$1" >&2
    exit 2
}

# Idempotent : un `exit` provoqué par un INT déjà trappé rejoue le trap EXIT. Les drapeaux
# évitent de lever deux fois un état déjà rendu au système.
cleanup() {
    if [ "$doze_forced" -eq 1 ]; then
        $ADB shell dumpsys deviceidle unforce >/dev/null 2>&1
        $ADB shell dumpsys battery reset >/dev/null 2>&1
        doze_forced=0
        log_info "Doze forcé levé, état de la batterie rendu au système."
    fi
}

# Bloc d'une seule alarme, de son en-tête à l'alarme suivante ; voir validate_blocking.sh pour la
# raison de l'exigence « en-tête immédiatement avant `tag=` ».
alarm_block() {
    $ADB shell dumpsys alarm 2>/dev/null | tr -d '\r' | awk -v tag="$1" '
        inblk { if (index($0, "Alarm{")) exit; print; next }
        hdrprev && index($0, tag) { print hdr; print; inblk = 1; next }
        { hdrprev = index($0, "Alarm{") > 0; if (hdrprev) hdr = $0 }
    '
}

# Epoch brut de l'en-tête `Alarm{... origWhen 1790240960149 ...}` ; voir validate_blocking.sh
# pour la raison pour laquelle la date formatée de la ligne de détail n'est jamais convertie.
alarm_when_ms() {
    local raw
    raw=$(alarm_block "$1" | head -1 | sed -n 's/.*origWhen \([0-9]\{13\}\) .*/\1/p')
    [ -n "$raw" ] || return 1
    printf '%s\n' "$raw"
}

device_now_ms() {
    $ADB shell date +%s%3N 2>/dev/null | tr -d '\r'
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

assert_hidden_from_next_alarm_clock() {
    local starts_at_ms="$1" section
    section=$($ADB shell dumpsys alarm 2>/dev/null | tr -d '\r' |
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

require_preconditions() {
    log_step "Préconditions"

    command -v "$ADB" >/dev/null 2>&1 || fatal "adb est introuvable (PATH ou SDK Android)."

    local devices
    devices=$($ADB devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
    [ "$devices" -eq 1 ] || fatal "$devices appareil(s) détecté(s) ; il en faut exactement un."
    log_pass "un appareil joignable"

    $ADB shell pm list packages 2>/dev/null | grep -qx "package:${NIUMI_PACKAGE}" ||
        fatal "${NIUMI_PACKAGE} n'est pas installé (./gradlew :app:installDebug)."
    log_pass "Niumi installé"

    require_device_clock

    local starts_at_ms now_ms margin_s required_s
    starts_at_ms=$(alarm_when_ms "$BLOCKING_START_TAG") ||
        fatal "aucune alarme de début en attente : armer une session à blocage différé d'abord."
    now_ms=$(device_now_ms)
    margin_s=$(( (starts_at_ms - now_ms) / 1000 ))
    required_s="$REBOOT_MIN_MARGIN_S"
    [ "$MODE" = "doze" ] && required_s="$DOZE_MIN_MARGIN_S"
    [ "$margin_s" -ge "$required_s" ] ||
        fatal "il reste ${margin_s} s avant l'instant de début ; il en faut ${required_s} pour ce mode."
    log_pass "alarme de début en attente, marge ${margin_s} s (origWhen=${starts_at_ms})"

    assert_hidden_from_next_alarm_clock "$starts_at_ms"

    log_info "Rappel non vérifiable ici : la permission OEM de démarrage automatique doit être"
    log_info "dans son état par défaut, REFUSÉ, sans quoi le résultat ne vaut pas pour un"
    log_info "utilisateur ordinaire (§20, §4.2). À consigner dans QA_MATRIX.md."

    STARTS_AT_MS="$starts_at_ms"
}

require_lock_screen() {
    local disabled
    disabled=$($ADB shell locksettings get-disabled 2>/dev/null | tr -d '\r')
    if [ "$disabled" != "false" ]; then
        fatal "aucun verrouillage d'écran (locksettings get-disabled = '${disabled}'). Poser un code : sans lui, l'utilisateur est déverrouillé seul au démarrage et l'essai ne prouve pas le chemin Direct Boot."
    fi
    log_pass "verrouillage d'écran en place"
}

await_boot_completed() {
    local deadline=$((SECONDS + BOOT_TIMEOUT_S)) completed
    while [ "$SECONDS" -lt "$deadline" ]; do
        completed=$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        [ "$completed" = "1" ] && return 0
        sleep 2
    done
    return 1
}

# L'utilisateur est-il encore verrouillé ? Deux sources, l'une corroborant l'autre : le stockage
# chiffré par les identifiants n'est pas disponible avant déverrouillage, et l'activité du système
# reste en RUNNING_LOCKED. Un état indéterminé est un échec, jamais un succès par défaut : un
# appareil déverrouillé ne prouverait pas le chemin Direct Boot.
assert_still_locked() {
    local ce_available activity_state
    ce_available=$($ADB shell getprop sys.user.0.ce_available 2>/dev/null | tr -d '\r')
    activity_state=$($ADB shell dumpsys activity 2>/dev/null | tr -d '\r' |
        grep -o 'RUNNING_LOCKED' | head -1)
    if [ "$ce_available" = "true" ] && [ -z "$activity_state" ]; then
        log_fail "appareil déverrouillé après le redémarrage : l'essai ne prouve pas la reprogrammation Direct Boot"
        return 1
    fi
    local locked_note="${activity_state}"
    [ -n "$locked_note" ] || locked_note="état d activité non lu"
    log_pass "appareil encore verrouillé (ce_available=${ce_available}, ${locked_note})"
    return 0
}

await_blocking_start_alarm() {
    local deadline=$((SECONDS + ALARM_RESTORE_TIMEOUT_S)) when
    while [ "$SECONDS" -lt "$deadline" ]; do
        when=$(alarm_when_ms "$BLOCKING_START_TAG") && {
            printf '%s\n' "$when"
            return 0
        }
        sleep 2
    done
    return 1
}

test_reboot_before_start_without_unlock() {
    log_step "Essai 1 — redémarrage avant l'heure de début, sans déverrouillage"
    require_lock_screen

    local before_start before_wake after_start after_wake boot_done_s restored_s
    before_start="$STARTS_AT_MS"
    before_wake=$(alarm_when_ms "$WAKE_ALARM_TAG") || before_wake=""
    if [ -n "$before_wake" ]; then
        log_pass "alarme de réveil relevée avant redémarrage (origWhen=${before_wake})"
    else
        log_fail "aucune alarme de réveil avant le redémarrage — la session n'est pas complète"
    fi

    printf '\n  \033[31mNE PAS DÉVERROUILLER après le redémarrage. Ne pas toucher l'\''écran.\033[0m\n'
    printf '       Appuyer sur Entrée pour redémarrer... '
    read -r _

    $ADB reboot
    $ADB wait-for-device
    # `wait-for-device` ne rend la main que sur adbd : le système, lui, n'est pas prêt.
    await_boot_completed || {
        log_fail "sys.boot_completed jamais atteint en ${BOOT_TIMEOUT_S} s"
        return
    }
    boot_done_s="$SECONDS"
    log_pass "démarrage terminé"

    assert_still_locked || return

    after_start=$(await_blocking_start_alarm) || {
        log_fail "alarme de début absente ${ALARM_RESTORE_TIMEOUT_S} s après le démarrage"
        return
    }
    restored_s=$((SECONDS - boot_done_s))
    log_pass "alarme de début reprogrammée ${restored_s} s après le démarrage, avant tout déverrouillage"

    # Comparaison de chaînes d'abord : §9.3 exige le MÊME instant, jamais recalculé. L'écart en
    # millisecondes ne sert qu'à diagnostiquer, un décalage d'une heure pile désignant le fuseau.
    if [ "$after_start" = "$before_start" ]; then
        log_pass "instant de début inchangé (${after_start})"
    else
        log_fail "instant de début déplacé : ${before_start} → ${after_start} (écart $((after_start - before_start)) ms)"
    fi

    after_wake=$(alarm_when_ms "$WAKE_ALARM_TAG") || after_wake=""
    if [ -n "$before_wake" ] && [ "$after_wake" = "$before_wake" ]; then
        log_pass "alarme de réveil intacte (${after_wake})"
    else
        log_fail "alarme de réveil absente ou déplacée après le redémarrage : '${after_wake}'"
    fi

    log_step "Essai 1b — application du blocage après déverrouillage"
    log_info "Déverrouiller le téléphone maintenant, puis revenir ici."
    printf '       Appuyer sur Entrée une fois déverrouillé... '
    read -r _
    await_blocking_delivery "$before_start"
}

# Preuve de délivrance : le bloc d'alarme disparaît du dump. Le journal technique de l'écran 12
# reste la mesure de référence du retard ; ce sondage est borné par son propre pas.
await_blocking_delivery() {
    local starts_at_ms="$1" deadline_s now_ms delay_s
    deadline_s=$(( (starts_at_ms - $(device_now_ms)) / 1000 + DOZE_MAX_DELAY_S + 30 ))
    [ "$deadline_s" -lt 30 ] && deadline_s=30
    log_info "attente de la délivrance (jusqu'à ${deadline_s} s)..."

    local deadline=$((SECONDS + deadline_s))
    while [ "$SECONDS" -lt "$deadline" ]; do
        if ! alarm_when_ms "$BLOCKING_START_TAG" >/dev/null 2>&1; then
            now_ms=$(device_now_ms)
            delay_s=$(( (now_ms - starts_at_ms) / 1000 ))
            log_pass "alarme de début délivrée, environ ${delay_s} s après l'instant contractuel"
            log_info "Mesure de référence : l'horodatage de BLOCKING_STARTED dans le journal technique"
            log_info "(écran 12). Ce sondage n'est précis qu'à ${DOZE_POLL_S} s près."
            DELIVERY_DELAY_S="$delay_s"
            return 0
        fi
        sleep "$DOZE_POLL_S"
    done
    log_fail "alarme de début toujours en attente ${deadline_s} s après l'instant contractuel"
    DELIVERY_DELAY_S=-1
    return 1
}

report_derogation_failure() {
    local delay_s="$1" doze_state="$2" reason="$3"
    printf '\n\033[31m!! SECONDE DÉROGATION §9.1 EN DÉFAUT\033[0m\n'
    printf '   Instant contractuel : %s    Retard observé : %s s\n' "$STARTS_AT_MS" "$delay_s"
    printf '   État Doze à la délivrance : %s    exactAllowReason : %s\n' "$doze_state" "$reason"
    printf '\n'
    printf '   setExactAndAllowWhileIdle() ne tient pas pour l'\''alarme de début du blocage sur cet\n'
    printf '   appareil. §9.1 fonde sa seconde dérogation sur le fait que USE_EXACT_ALARM affranchit\n'
    printf '   l'\''alarme des politiques device_idle et app_standby, mesuré sur le watchdog le\n'
    printf '   2026-09-15. La mesure ci-dessus la contredit.\n'
    printf '\n'
    printf '   NE PAS POURSUIVRE l'\''étape 25. Suite attendue : revenir sur §9.1 et ARBITRER AVEC\n'
    printf '   L'\''UTILISATEUR l'\''option setAlarmClock() avec showPendingIntent vers l'\''écran 7, au prix\n'
    printf '   de l'\''affichage « prochaine alarme » pour un instant où Niumi ne sonnera pas. C'\''est une\n'
    printf '   décision produit, pas technique : ni ce script ni le code ne la prennent.\n'
    printf '\n'
    printf '   À consigner dans QA_MATRIX.md, ligne « blocage différé, Doze forcé à l'\''heure de début »,\n'
    printf '   avec fabricant, modèle, Android, firmware, état Doze, retard et le bloc dumpsys.\n\n'
    failures=$((failures + 1))
}

test_forced_doze_at_start() {
    log_step "Essai 2 — Doze profond forcé à l'heure de début"

    # Garde de transport : câble branché, `force-idle` s'arrête à INACTIVE et l'essai serait nul
    # par construction. Mieux vaut refuser que produire un « non concluant » déguisé en échec.
    if $ADB shell dumpsys battery 2>/dev/null | tr -d '\r' | grep -qE 'USB powered: true|AC powered: true'; then
        printf '\n'
        printf '  L'\''appareil est en charge : le Doze profond est inatteignable dans cet état.\n'
        printf '  Basculer sur adb par Wi-Fi, câble retiré, puis relancer ce seul essai :\n\n'
        printf '      adb tcpip 5555\n'
        printf '      adb shell ip route            # relever l'\''adresse IP\n'
        printf '      (retirer le câble USB)\n'
        printf '      adb connect <ip>:5555\n'
        printf '      ADB="adb -s <ip>:5555" tools/validate_blocking_start.sh doze\n\n'
        fatal "essai Doze impossible câble branché (mesuré à la porte 0, voir LOT-0.md)."
    fi

    $ADB shell dumpsys battery unplug >/dev/null 2>&1
    doze_forced=1
    $ADB shell dumpsys deviceidle force-idle >/dev/null 2>&1

    local deep step=0
    deep=$($ADB shell dumpsys deviceidle get deep 2>/dev/null | tr -d '\r')
    while [ "$deep" != "IDLE" ] && [ "$step" -lt "$DOZE_STEP_MAX" ]; do
        $ADB shell dumpsys deviceidle step deep >/dev/null 2>&1
        deep=$($ADB shell dumpsys deviceidle get deep 2>/dev/null | tr -d '\r')
        step=$((step + 1))
    done

    if [ "$deep" != "IDLE" ]; then
        log_fail "Doze profond non atteint (état='${deep}') — essai NON CONCLUANT, ne rien conclure sur §9.1"
        return
    fi
    log_pass "Doze profond atteint après ${step} pas"

    local reason
    reason=$(alarm_block "$BLOCKING_START_TAG" |
        sed -n 's/.*exactAllowReason=\([a-z_]*\).*/\1/p' | head -1)
    log_info "exactAllowReason=${reason:-non lu}"

    await_blocking_delivery "$STARTS_AT_MS"
    local delay_s="$DELIVERY_DELAY_S"
    deep=$($ADB shell dumpsys deviceidle get deep 2>/dev/null | tr -d '\r')

    if [ "$delay_s" -lt 0 ]; then
        report_derogation_failure "non délivrée" "$deep" "${reason:-non lu}"
        return
    fi
    if [ "$deep" != "IDLE" ]; then
        log_info "L'appareil est sorti du Doze profond (état='${deep}') : la délivrance a pu bénéficier"
        log_info "de cette sortie, l'essai ne prouve alors rien. Réessayer sans interaction."
    fi
    if [ "$delay_s" -gt "$DOZE_MAX_DELAY_S" ] || [ "${reason:-}" != "policy_permission" ]; then
        report_derogation_failure "$delay_s" "$deep" "${reason:-non lu}"
        return
    fi
    log_pass "dérogation §9.1 confirmée sur cet appareil : délivrance à ${delay_s} s en Doze profond"
}

main() {
    case "$MODE" in
        reboot|doze|all) ;;
        *)
            printf 'Usage : %s [reboot|doze|all]\n' "$0" >&2
            exit 2
            ;;
    esac

    # Trap posé AVANT toute commande susceptible de forcer un état : entre `battery unplug` et la
    # pose du trap, un Ctrl-C laisserait l'appareil persuadé d'être débranché jusqu'au prochain
    # redémarrage.
    trap cleanup EXIT INT TERM

    require_preconditions

    if [ "$MODE" = "reboot" ] || [ "$MODE" = "all" ]; then
        test_reboot_before_start_without_unlock
    fi

    if [ "$MODE" = "all" ]; then
        log_step "Bascule de transport requise"
        log_info "L'essai Doze exige adb par Wi-Fi, câble retiré (voir l'en-tête de ce script)."
        log_info "Réarmer une session différée, puis relancer : tools/validate_blocking_start.sh doze"
    elif [ "$MODE" = "doze" ]; then
        test_forced_doze_at_start
    fi

    log_step "Résultat"
    if [ "$failures" -eq 0 ]; then
        printf '  Tous les essais de ce mode sont passés.\n'
        printf '  Penser à terminer la session par un scan du boîtier (§3 : aucune autre sortie).\n'
        exit 0
    fi
    printf '  \033[31m%s essai(s) en échec.\033[0m\n' "$failures"
    exit 1
}

main "$@"
