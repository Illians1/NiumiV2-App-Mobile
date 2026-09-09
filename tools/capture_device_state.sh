#!/usr/bin/env bash
#
# Relevé de l'état d'un appareil Android pour la matrice de tests physiques (SPEC_ANDROID §20).
#
# §20 impose de consigner, pour chaque essai : le fabricant, le modèle, la version Android, la
# version du firmware, les permissions, le résultat, le retard mesuré et les logs locaux. Ce
# script produit les colonnes de contexte (tout sauf le résultat et le retard, qui viennent de
# l'essai lui-même), afin qu'elles ne soient ni approximées ni recopiées de mémoire d'un essai
# à l'autre.
#
# Il ne pilote rien, ne modifie aucun réglage et ne remplace aucun essai : il lit et affiche.
#
# Usage : tools/capture_device_state.sh [package]   (défaut : com.niumi.app)

# Pas de `pipefail` ni de `-e` : le script enchaîne des pipelines vers `grep -m1` et `head`, qui
# ferment le tuyau en amont (SIGPIPE) et feraient avorter un relevé pourtant correct. Les seules
# erreurs qui doivent arrêter le script sont les préconditions, traitées explicitement ci-dessous.
set -u

PACKAGE="${1:-com.niumi.app}"
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
    "$ADB" devices >&2
    exit 1
fi

prop() { "$ADB" shell getprop "$1" | tr -d '\r'; }
setting() { "$ADB" shell settings get "$1" "$2" 2>/dev/null | tr -d '\r'; }

echo "## Contexte appareil — $(date '+%Y-%m-%d %H:%M:%S')"
echo
echo "| Champ | Valeur |"
echo "| --- | --- |"
echo "| Fabricant | $(prop ro.product.manufacturer) |"
echo "| Modèle | $(prop ro.product.model) |"
echo "| Android | $(prop ro.build.version.release) / API $(prop ro.build.version.sdk) |"
echo "| Firmware | $(prop ro.build.display.id) |"
echo "| Surcouche | $(prop ro.miui.ui.version.name 2>/dev/null || true) $(prop ro.mi.os.version.name 2>/dev/null || true) |"
echo "| Version de Niumi | $("$ADB" shell dumpsys package "$PACKAGE" | awk -F= '/versionName/{print $2; exit}' | tr -d '\r') |"

echo
echo "### Permissions et accès sensibles"
echo
echo "| Élément | État |"
echo "| --- | --- |"
"$ADB" shell dumpsys package "$PACKAGE" \
    | awk '/runtime permissions:/,/^$/' \
    | grep -E "POST_NOTIFICATIONS|NFC" \
    | sed 's/^[[:space:]]*/| /; s/: granted=/ | /; s/,.*/ |/' \
    | tr -d '\r' || true
echo "| Notifications autorisées (app) | $("$ADB" shell cmd notification allowed_listeners >/dev/null 2>&1; "$ADB" shell dumpsys notification --noredact | grep -c "pkg=$PACKAGE" | tr -d '\r') notification(s) postée(s) actuellement |"
echo "| Service d'accessibilité Niumi | $(setting secure enabled_accessibility_services | grep -q "$PACKAGE" && echo "actif" || echo "INACTIF") |"
echo "| accessibility_enabled | $(setting secure accessibility_enabled) |"
echo "| Exemption batterie AOSP | $("$ADB" shell dumpsys deviceidle whitelist | grep -q "$PACKAGE" && echo "oui" || echo "non (peut rester non sur les surcouches, voir §13)") |"

echo
echo "### Audio et interruptions"
echo
echo "| Élément | État |"
echo "| --- | --- |"
volumes=$("$ADB" shell dumpsys audio | tr -d '\r')
for stream in STREAM_ALARM STREAM_MUSIC STREAM_NOTIFICATION STREAM_RING; do
    # Un flux peut être aliasé vers un autre (sur HyperOS, STREAM_NOTIFICATION est aliasé vers
    # STREAM_RING) : le noter, la distinction compte pour les essais de volume de §20.
    header=$(echo "$volumes" | grep -m1 -E "^- $stream( \(aliased to: [A-Z_]+\))?:" || true)
    alias=$(echo "$header" | sed -n 's/.*aliased to: \([A-Z_]*\).*/ (aliasé vers \1)/p')
    line=$(echo "$volumes" | grep -A6 -- "$header" | grep -m1 -E "streamVolume" | sed 's/^[[:space:]]*//' || true)
    echo "| $stream${alias} | ${line:-non lu} |"
done
ringer=$(echo "$volumes" | awk -F'= ' '/^- mode \(internal\)/ {print $2; exit}')
echo "| Mode sonnerie | ${ringer:-non lu} |"
echo "| Ne pas déranger (zen_mode) | $(setting global zen_mode) — 0 désactivé, 1 alarmes prioritaires, 2 silence total, 3 alarmes seules |"

echo
echo "### Énergie"
echo
echo "| Élément | État |"
echo "| --- | --- |"
echo "| État Doze | $("$ADB" shell dumpsys deviceidle get deep | tr -d '\r') |"
echo "| Économie d'énergie | $(setting global low_power) |"

echo
echo "### Alarme programmée par Niumi"
echo
alarms=$("$ADB" shell dumpsys alarm | tr -d '\r' | grep -B2 -A6 "$PACKAGE" | head -40 || true)
echo "${alarms:-(aucune alarme trouvée pour $PACKAGE)}"
