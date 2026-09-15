#!/usr/bin/env bash
#
# Protocole de mesure d'un déclenchement d'alarme (SPEC_ANDROID §9.1, §10.1 à §10.4, §20).
#
# §20 impose de consigner pour chaque essai le résultat ET le retard mesuré. Ce script produit
# ces deux colonnes objectivement, plutôt que de s'en remettre au chronomètre de l'opérateur :
# il lit l'instant cible réel dans `dumpsys alarm`, détecte le démarrage de `AlarmRingingService`
# par sondage, puis relève l'état du service, du lecteur audio, de la notification et de
# l'activité au premier plan.
#
# L'armement se fait à la main dans l'application (étape 21 : la route POC de debug, que ce
# script pilotait par `input tap`, a été supprimée avec le reste du Lot 0). Il n'existe aucun
# autre moyen d'armer une session : §9.2 impose l'activation en deux phases par le parcours
# utilisateur réel, et aucun raccourci de debug ne doit exister en dehors des tests (CLAUDE.md).
#
# Ce qu'il ne vérifie PAS : l'audibilité réelle du son. `dumpsys` prouve qu'un lecteur en
# USAGE_ALARM est démarré et que le flux d'alarme n'est pas muté, pas qu'un son sort du
# haut-parleur. Cette confirmation reste à l'oreille de l'opérateur, et doit être consignée
# comme telle dans la matrice.
#
# Préconditions (vérifiées, le script s'arrête sinon) :
#   1. un seul appareil branché, débogage USB activé ;
#   2. Niumi installé (`./gradlew :app:installDebug`, ou APK release) ;
#   3. une session armée dans l'application, dont l'alarme est visible dans `dumpsys alarm`.
#
# L'arrêt de la sonnerie n'est pas scriptable : le produit n'expose aucune action d'arrêt
# (SPEC_ANDROID §3, §10.2). Elle se termine par un scan du boîtier associé, à faire à la main.
#
# Usage : tools/validate_alarm.sh [attente_max_secondes]   (défaut : 900)

set -u

MAX_WAIT="${1:-900}"
PACKAGE="com.niumi.app"
ADB="${ADB:-adb}"

if ! command -v "$ADB" >/dev/null 2>&1; then
    if [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
        ADB="$HOME/Library/Android/sdk/platform-tools/adb"
    else
        echo "adb introuvable. Renseigner ADB=/chemin/vers/adb." >&2
        exit 1
    fi
fi

device_count=$("$ADB" devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
if [ "$device_count" != "1" ]; then
    echo "Précondition manquante : exactement un appareil branché est attendu, $device_count trouvé(s)." >&2
    exit 1
fi

if ! "$ADB" shell pm list packages | tr -d '\r' | grep -qx "package:$PACKAGE"; then
    echo "Précondition manquante : $PACKAGE n'est pas installé." >&2
    exit 1
fi

# Bloc `dumpsys alarm` de l'alarme de réveil de Niumi. Le watchdog de `RINGING` (§9.1, étape 20)
# vise `RingingWatchdogReceiver` et ne doit jamais être confondu avec le réveil : on ne retient
# que les entrées `RTC_WAKEUP` qui portent `AlarmReceiver`.
alarm_block() {
    "$ADB" shell dumpsys alarm 2>/dev/null | tr -d '\r' |
        grep -B4 "$PACKAGE/com.niumi.system.alarm.AlarmReceiver" |
        grep -m1 "when="
}

echo "== Précondition : session armée =="
echo "  Armer une session dans Niumi (parcours complet : diagnostic, boîtier, applications,"
echo "  heure de réveil la plus proche possible, activation), puis revenir ici."
printf '  Appuyer sur Entrée une fois la session armée... '
read -r _

target_line=$(alarm_block)
if [ -z "$target_line" ]; then
    echo "Aucune alarme de réveil Niumi trouvée dans dumpsys alarm : la session n'est pas armée." >&2
    exit 1
fi
echo "Alarme programmée : $target_line"

# `when=` donne l'instant cible en millisecondes depuis l'epoch : c'est la référence du retard,
# et elle vient du système plutôt que d'un délai saisi par l'opérateur.
target_ms=$(printf '%s\n' "$target_line" | sed -n 's/.*when=\([0-9]\{10,\}\).*/\1/p')
if [ -z "$target_ms" ]; then
    echo "Instant cible illisible dans dumpsys alarm ; le retard ne pourra pas être mesuré." >&2
fi

started_at_ms=$("$ADB" shell date +%s%3N | tr -d '\r')
deadline_ms=$((started_at_ms + MAX_WAIT * 1000))

echo "== Attente du déclenchement (limite : ${MAX_WAIT} s) =="
fired_ms=""
while :; do
    now_ms=$("$ADB" shell date +%s%3N | tr -d '\r')
    if "$ADB" shell dumpsys activity services "$PACKAGE" 2>/dev/null | tr -d '\r' | grep -q "AlarmRingingService"; then
        fired_ms="$now_ms"
        break
    fi
    if [ "$now_ms" -gt "$deadline_ms" ]; then
        echo "ÉCHEC : aucun AlarmRingingService démarré dans la fenêtre d'attente."
        break
    fi
    sleep 2
done

echo
echo "## Résultat"
echo
if [ -n "$fired_ms" ] && [ -n "$target_ms" ]; then
    # Le sondage a un pas de 2 s : le retard mesuré est donc précis à ± 2 s près.
    delay=$(( (fired_ms - target_ms) / 1000 ))
    echo "- Service détecté au premier plan ${delay} s après l'instant cible (sondage à ± 2 s)."
elif [ -n "$fired_ms" ]; then
    echo "- Service détecté au premier plan, retard non mesurable (instant cible illisible)."
else
    echo "- Service jamais détecté."
fi
echo "- Service : $("$ADB" shell dumpsys activity services "$PACKAGE" 2>/dev/null | tr -d '\r' | grep -m1 -E "isForeground|ServiceRecord.*AlarmRingingService" | sed 's/^[[:space:]]*//')"
echo "- Lecteur audio : $("$ADB" shell dumpsys audio 2>/dev/null | tr -d '\r' | grep -m1 -E "usage=USAGE_ALARM" | sed 's/^[[:space:]]*//')"
echo "- Flux alarme muté : $("$ADB" shell dumpsys audio 2>/dev/null | tr -d '\r' | sed -n '/^- STREAM_ALARM:/,/^$/p' | grep -m1 "Muted:")"
echo "- Volume alarme : $("$ADB" shell cmd audio get-stream-volume 4 2>/dev/null | tr -d '\r' | sed 's/.*-> //')"
echo "- Notification : $("$ADB" shell dumpsys notification --noredact 2>/dev/null | tr -d '\r' | grep -m1 -A2 "pkg=$PACKAGE" | tr '\n' ' ')"
echo "- Activité au premier plan : $("$ADB" shell dumpsys activity activities 2>/dev/null | tr -d '\r' | grep -m1 "topResumedActivity" | sed 's/^[[:space:]]*//')"
echo
echo "À confirmer à l'oreille : le son est-il réellement audible ? Puis scanner le boîtier"
echo "associé pour terminer la session (aucune action d'arrêt logicielle n'existe, §3)."
