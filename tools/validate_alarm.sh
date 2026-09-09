#!/usr/bin/env bash
#
# Protocole de mesure d'un déclenchement d'alarme (SPEC_ANDROID §9.1, §10.1 à §10.4, §20).
#
# §20 impose de consigner pour chaque essai le résultat ET le retard mesuré. Ce script produit
# ces deux colonnes objectivement, plutôt que de s'en remettre au chronomètre de l'opérateur :
# il programme l'alarme depuis la route POC de debug, lit l'instant cible dans `dumpsys alarm`,
# détecte le démarrage de `AlarmRingingService` par sondage, puis relève l'état du service, du
# lecteur audio, de la notification et de l'activité au premier plan.
#
# Ce qu'il ne vérifie PAS : l'audibilité réelle du son. `dumpsys` prouve qu'un lecteur en
# USAGE_ALARM est démarré et que le flux d'alarme n'est pas muté, pas qu'un son sort du
# haut-parleur. Cette confirmation reste à l'oreille de l'opérateur, et doit être consignée
# comme telle dans la matrice.
#
# Préconditions (vérifiées, le script s'arrête sinon) :
#   1. un seul appareil branché, débogage USB activé ;
#   2. Niumi debug installé (`./gradlew :app:installDebug`) ;
#   3. la route POC ouverte à l'écran, ou l'application lançable (le script l'ouvre).
#
# L'arrêt de la sonnerie n'est pas scriptable : le produit n'expose aucune action d'arrêt
# (SPEC_ANDROID §3, §10.2). Elle se termine par un scan du boîtier associé, à faire à la main.
#
# Usage : tools/validate_alarm.sh [délai_secondes]   (défaut : 30)

set -u

DELAY="${1:-30}"
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

ui_dump() {
    "$ADB" shell uiautomator dump /sdcard/niumi_ui.xml >/dev/null 2>&1
    "$ADB" shell cat /sdcard/niumi_ui.xml | tr -d '\r'
}

# Centre du premier nœud dont le texte vaut exactement $1, au format "x y" ; vide si absent.
node_center() {
    ui_dump | tr '<' '\n' | grep "text=\"$1\"" | head -1 | sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\].*/\1 \2 \3 \4/p' \
        | awk '{ if (NF==4) print int(($1+$3)/2), int(($2+$4)/2) }'
}

echo "== Ouverture de la route POC =="
"$ADB" shell am start -n "$PACKAGE/.MainActivity" >/dev/null 2>&1
sleep 2
poc=$(node_center "POC alarme (debug)")
if [ -n "$poc" ]; then
    # Sur l'accueil, ce libellé est celui du bouton ; sur l'écran POC, celui du titre.
    if [ -z "$(node_center 'Programmer')" ]; then
        # shellcheck disable=SC2086
        "$ADB" shell input tap $poc >/dev/null 2>&1
        sleep 2
    fi
fi

programmer=$(node_center "Programmer")
if [ -z "$programmer" ]; then
    echo "Écran POC introuvable : le bouton « Programmer » n'est pas affiché." >&2
    exit 1
fi

echo "== Saisie du délai : ${DELAY} s =="
champ=$(ui_dump | tr '<' '\n' | grep 'class="android.widget.EditText"' | head -1 \
    | sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\].*/\1 \2 \3 \4/p' \
    | awk '{ print int(($1+$3)/2), int(($2+$4)/2) }')
if [ -n "$champ" ]; then
    # shellcheck disable=SC2086
    "$ADB" shell input tap $champ >/dev/null 2>&1
    sleep 1
    "$ADB" shell input keyevent KEYCODE_MOVE_END >/dev/null 2>&1
    for _ in 1 2 3 4 5; do "$ADB" shell input keyevent KEYCODE_DEL >/dev/null 2>&1; done
    "$ADB" shell input text "$DELAY" >/dev/null 2>&1
    "$ADB" shell input keyevent KEYCODE_BACK >/dev/null 2>&1   # referme le clavier
    sleep 1
fi

echo "== Programmation =="
programmer=$(node_center "Programmer")
# shellcheck disable=SC2086
"$ADB" shell input tap $programmer >/dev/null 2>&1
sleep 2

target_line=$("$ADB" shell dumpsys alarm | tr -d '\r' | grep -A3 "$PACKAGE" | grep -m1 "when=")
echo "Alarme programmée : ${target_line:-non trouvée dans dumpsys alarm}"
started_at_ms=$("$ADB" shell date +%s%3N | tr -d '\r')
deadline_ms=$((started_at_ms + (DELAY + 120) * 1000))

echo "== Attente du déclenchement (limite : délai + 120 s) =="
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
if [ -n "$fired_ms" ]; then
    elapsed=$(( (fired_ms - started_at_ms) / 1000 ))
    # Le sondage a un pas de 2 s : le retard mesuré est donc précis à ± 2 s près.
    echo "- Service détecté au premier plan après ${elapsed} s (délai demandé : ${DELAY} s, sondage à ± 2 s)."
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
