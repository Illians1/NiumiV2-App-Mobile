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
# Ce qu'il ne vérifie PAS : l'audibilité réelle du son, et le ressenti de la montée progressive
# ou de la boucle. `dumpsys` prouve qu'un lecteur en USAGE_ALARM est démarré et que le flux
# d'alarme n'est pas muté, pas qu'un son sort du haut-parleur ; le logcat (ci-dessous) prouve la
# série d'amplitudes et les reprises de boucle, pas qu'elles sont inaudibles ou sans blanc à
# l'oreille. Ces deux confirmations restent à l'opérateur, et doivent être consignées comme
# telles dans la matrice.
#
# Préconditions (vérifiées, le script s'arrête sinon) :
#   1. un seul appareil branché, débogage USB activé ;
#   2. Niumi installé (`./gradlew :app:installDebug`, ou APK release) ;
#   3. une session armée dans l'application, dont l'alarme est visible dans `dumpsys alarm`.
#
# L'arrêt de la sonnerie n'est pas scriptable : le produit n'expose aucune action d'arrêt
# (SPEC_ANDROID §3, §10.2). Elle se termine par un scan du boîtier associé, à faire à la main.
#
# Sonneries et volume progressif (Lot 7, étape 28). Le script a besoin de connaître la clé de
# sonnerie et la durée de montée de la session armée pour savoir QUOI mesurer (durée du fichier,
# amplitude théorique à chaque instant) :
#   - en debug, il les lit directement dans la projection Direct Boot sur l'appareil
#     (`run-as … cat`, device-protected storage, lisible même avant déverrouillage comme la
#     projection elle-même, SPEC_ANDROID §7.3) ; c'est la source qui fait foi, puisqu'elle ne
#     peut pas se tromper sur ce qui va réellement sonner ;
#   - si cette lecture échoue (APK release, où `run-as` est refusé), les options --ringtone et
#     --ramp permettent de les donner à la main. Le script dit toujours laquelle des deux
#     sources il a utilisée, jamais une valeur supposée en silence.
#
# Usage : tools/validate_alarm.sh [--ringtone <clé>] [--ramp <secondes|none>] [attente_max_secondes]
# Exemple : tools/validate_alarm.sh --ringtone niumi_oiseaux --ramp 300 900

set -u

readonly PACKAGE="com.niumi.app"
# Receveur de l'alarme de réveil (`:feature:ringing`). Jusqu'au 2026-09-30, le script cherchait
# `com.niumi.system.alarm.AlarmReceiver`, qui n'existe pas : aucune session n'était jamais trouvée.
readonly WAKE_ALARM_TAG="$PACKAGE/com.niumi.feature.ringing.AlarmReceiver"
# Chemin de la projection Direct Boot (FileDirectBootStore, `:core:database`) : device-protected
# storage de l'utilisateur 0, lisible via `run-as` en variante debug uniquement. Jamais vérifié
# sur appareil avant cette étape : si le chemin est faux sur un appareil donné, la lecture échoue
# proprement (chaîne vide) et le script retombe sur --ringtone/--ramp, voir read_session_extras.
readonly DIRECT_BOOT_PATH="/data/user_de/0/$PACKAGE/files/niumi_session.json"
# Dossier des quatre sonneries normalisées (Lot 7, étape 26) : PCM 16 bits, mono, 44,1 kHz,
# garanti par NiumiRingtoneWavTest. 88 200 = 44 100 Hz × 1 canal × 2 octets/échantillon.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPO_ROOT
readonly RAW_DIR="$REPO_ROOT/androidApp/feature/ringing/src/main/res/raw"
readonly WAV_BYTES_PER_SECOND=88200
# VolumeRampPolicy (`:core:system`) : pas de 250 ms, −40 dB au départ (0.01), 0 dB à la durée.
readonly RAMP_TICK_MS=250
ADB="${ADB:-adb}"

if ! command -v "$ADB" >/dev/null 2>&1; then
    if [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
        ADB="$HOME/Library/Android/sdk/platform-tools/adb"
    fi
fi

# ---------------------------------------------------------------------------
# Fonctions pures (sans adb), testables en les sourçant : voir la garde en bas de fichier qui
# empêche main() de s'exécuter à la place d'un test. Aucune ne dépend d'un appareil branché.
# ---------------------------------------------------------------------------

# Durée en secondes (deux décimales) du fichier res/raw/<clé>.wav, lue dans son en-tête RIFF
# plutôt que recopiée en dur : une future renormalisation (point de vigilance 14/15 du plan) ne
# pourra jamais faire mentir ce script. Cherche le premier sous-bloc "data" (sa taille suit
# immédiatement ses quatre octets ASCII) : la position fixe (offset 40) observée aujourd'hui sur
# les quatre fichiers n'est pas garantie si un en-tête gagne un sous-bloc "LIST" un jour.
wav_duration_seconds() {
    local key="$1" path hex marker idx byte_offset size_hex b1 b2 b3 b4 data_size file_size
    path="$RAW_DIR/${key}.wav"
    [ -f "$path" ] || { printf ''; return 1; }
    hex=$(xxd -p "$path" 2>/dev/null | tr -d '\n')
    marker="64617461" # "data" en ASCII
    idx=${hex%%"$marker"*}
    if [ "$idx" = "$hex" ]; then
        printf ''
        return 1
    fi
    byte_offset=$(( ${#idx} / 2 ))
    size_hex=${hex:$(( (byte_offset + 4) * 2 )):8}
    [ ${#size_hex} -eq 8 ] || { printf ''; return 1; }
    b1=${size_hex:0:2}; b2=${size_hex:2:2}; b3=${size_hex:4:2}; b4=${size_hex:6:2}
    data_size=$(( 16#${b4}${b3}${b2}${b1} ))
    file_size=$(( ${#hex} / 2 ))
    if [ "$data_size" -le 0 ] || [ "$data_size" -gt "$file_size" ]; then
        printf ''
        return 1
    fi
    awk -v n="$data_size" -v r="$WAV_BYTES_PER_SECOND" 'BEGIN { printf "%.2f", n / r }'
}

# Amplitude attendue du lecteur à `elapsed_ms` d'écoulé pour une montée de `duration_ms`
# (VolumeRampPolicy.amplitudeAt, `:core:system`) : 10^(−2·(1−p)), p borné à [0, 1].
# `duration_ms` ≤ 0 → 1 (volume constant), même règle que le moteur.
amplitude_at() {
    local elapsed_ms="$1" duration_ms="$2"
    awk -v e="$elapsed_ms" -v d="$duration_ms" 'BEGIN {
        if (d <= 0) { printf "1.000000"; exit }
        p = e / d
        if (p < 0) p = 0
        if (p > 1) p = 1
        printf "%.6f", exp((-2 * (1 - p)) * log(10))
    }'
}

# Lit un champ d'un objet JSON plat à un seul niveau (celui de la projection Direct Boot,
# DirectBootSnapshot.Active : aucun champ utile au script n'est imbriqué). Une valeur absente ou
# `null` renvoie une chaîne vide, jamais une valeur par défaut inventée : c'est à l'appelant de
# décider quoi faire d'un champ qu'il n'a pas pu lire.
read_json_field() {
    local json="$1" field="$2" match
    match=$(printf '%s' "$json" | grep -o "\"$field\":\(null\|\"[^\"]*\"\|-\{0,1\}[0-9][0-9]*\)" | head -1)
    match=${match#*:}
    if [ "$match" = "null" ] || [ -z "$match" ]; then
        printf ''
        return 0
    fi
    match="${match%\"}"
    match="${match#\"}"
    printf '%s' "$match"
}

# Bloc `dumpsys alarm` d'une seule alarme, de son en-tête `Alarm{...}` à l'alarme suivante. Le
# watchdog de `RINGING` (§9.1, étape 20) vise `RingingWatchdogReceiver` et ne doit jamais être
# confondu avec le réveil : seul le tag exact du receveur est retenu. L'en-tête doit précéder
# IMMÉDIATEMENT la ligne `tag=` — le nom du receveur réapparaît dans les statistiques du dump, où
# un `grep -B` ramasserait l'en-tête d'une autre alarme (même fonction que validate_blocking.sh et
# validate_blocking_start.sh).
alarm_block() {
    "$ADB" shell dumpsys alarm 2>/dev/null | tr -d '\r' | awk -v tag="$1" '
        inblk { if (index($0, "Alarm{")) exit; print; next }
        hdrprev && index($0, tag) { print hdr; print; inblk = 1; next }
        { hdrprev = index($0, "Alarm{") > 0; if (hdrprev) hdr = $0 }
    '
}

# Epoch brut de l'en-tête `Alarm{... origWhen 1790240960149 ...}`. `dumpsys alarm` n'affiche pas
# de `when=<epoch>`, et la date formatée de la ligne de détail (`origWhen=2026-09-24 11:09:20.149`)
# ne se convertit qu'avec `date -D` sur l'appareil, qui ignore l'heure d'été (décalage d'une heure
# mesuré le 2026-09-24, voir validate_blocking.sh).
alarm_when_ms() {
    local raw
    raw=$(alarm_block "$1" | head -1 | sed -n 's/.*origWhen \([0-9]\{13\}\) .*/\1/p')
    [ -n "$raw" ] || return 1
    printf '%s\n' "$raw"
}

# Lit ringtoneKey, volumeRampSeconds et ringingAtEpochMillis depuis la projection Direct Boot sur
# l'appareil (debug seulement, voir DIRECT_BOOT_PATH). Remplit les variables globales
# SESSION_RINGTONE_KEY, SESSION_RAMP_SECONDS (vide = constant) et SESSION_RINGING_AT_MS (vide si
# pas encore sonné ou illisible), et SESSION_SOURCE ("projection" ou "options"). N'écrase jamais
# une valeur déjà donnée par --ringtone/--ramp si la projection est illisible : c'est l'appelant
# (main) qui décide de l'ordre de préférence.
read_session_extras() {
    local json
    json=$("$ADB" shell "run-as $PACKAGE cat $DIRECT_BOOT_PATH" 2>/dev/null | tr -d '\r')
    if [ -z "$json" ] || ! printf '%s' "$json" | grep -q '"sessionId"'; then
        SESSION_PROJECTION_JSON=""
        return 1
    fi
    SESSION_PROJECTION_JSON="$json"
    return 0
}

# Analyse le logcat déjà capturé (fichier passé en argument, format `logcat -v epoch`) pour la
# fenêtre de sonnerie : série des `setVolume` de la piste d'alarme, et reprises de boucle.
#
# Mesuré sur le Xiaomi le 2026-10-06 (première vraie capture, session A de l'étape 28) : le flux
# `AudioTrack` est **partagé par tout le système**, pas seulement par Niumi (musique, NFC,
# notifications d'autres applications s'y mêlent), et la ligne `AudioTrack::setVolume L:… R:…
# sessionid=<n>` ne porte elle-même aucun nom de paquet. Isoler Niumi exige de suivre, ligne à
# ligne, le dernier paquet annoncé par les lignes voisines qui en portent un — observées sous deux
# formes : « AudioTrackImpl: before scaleVolumeForVideo … and the pakage is <pkg> » (sic, faute du
# fournisseur) juste avant `setVolume`, et « AudioTrack: start(...): … packageName: <pkg> » juste
# après. Un simple `grep` sur « setVolume », essayé en premier, ramenait des lignes bien formées
# mais aucune n'était distinguable de celles d'un autre flux : 0 valeur reconnue sur la première
# campagne, malgré des lignes manifestement présentes dans le brut. Voir ETAPE-28.md.
#
# Second défaut trouvé le 2026-10-07 : l'extraction du timestamp ne l'ancre plus en tête de ligne
# (`match($0, /[0-9]+\.[0-9]+/)`, plus `/^[0-9]+\.[0-9]+/`). Un `adb logcat` sans `-d` (flux
# continu, utilisé pour la mesure manuelle de secours ce jour-là) préfixe chaque ligne d'espaces de
# cadrage que `-d` (dump, utilisé par ce script) ne produit pas de façon identique ; l'ancre
# ratait alors le timestamp sur 100 % des lignes. Le premier nombre à virgule de la ligne reste le
# timestamp dans les deux cas, donc l'ancre n'apportait rien et sa suppression couvre les deux
# formes sans rien perdre.
readonly RAMP_MIN_POINTS=5
niumi_tagged_lines() {
    local logfile="$1" awk_program="$2"
    awk -v want="com.niumi.app" '
        function package_of(line) {
            if (match(line, /and the pakage is [A-Za-z0-9_.]+/)) {
                s = substr(line, RSTART, RLENGTH)
                sub(/.*is /, "", s)
                return s
            }
            if (match(line, /packageName: *[A-Za-z0-9_.]+/)) {
                s = substr(line, RSTART, RLENGTH)
                sub(/.*: */, "", s)
                return s
            }
            return ""
        }
        {
            p = package_of($0)
            if (p != "") cur_pkg = p
        }
        '"$awk_program"'
    ' "$logfile" 2>/dev/null
}

analyze_ramp_logcat() {
    local logfile="$1"
    niumi_tagged_lines "$logfile" '
        cur_pkg == want && match($0, /[Aa]udio[Tt]rack.*[Ss]et[Vv]olume/) {
            ts = ""; val = ""
            if (match($0, /[0-9]+\.[0-9]+/)) ts = substr($0, RSTART, RLENGTH)
            if (match($0, /L:-?[0-9]+\.[0-9]+/)) val = substr($0, RSTART + 2, RLENGTH - 2)
            if (ts != "" && val != "") print ts, val
        }
    '
}

analyze_loop_logcat() {
    local logfile="$1"
    niumi_tagged_lines "$logfile" '
        cur_pkg == want && match($0, /[Aa]udio[Tt]rack:.*\(.*\):/) && $0 ~ /start\(|stop\(|pause\(|flush\(/ {
            if (match($0, /[0-9]+\.[0-9]+/)) print substr($0, RSTART, RLENGTH)
        }
    '
}

# ---------------------------------------------------------------------------
# Programme principal : tout ce qui touche à l'appareil vit ici, pour que les fonctions pures
# ci-dessus restent sourçables et testables sans adb ni session armée (bash -n, puis un harnais
# jetable qui source ce fichier et n'appelle jamais main).
# ---------------------------------------------------------------------------
main() {
    local max_wait=900
    local cli_ringtone="" cli_ramp_arg=""
    while [ $# -gt 0 ]; do
        case "$1" in
            --ringtone)
                cli_ringtone="${2:-}"
                shift 2
                ;;
            --ramp)
                cli_ramp_arg="${2:-}"
                shift 2
                ;;
            *)
                max_wait="$1"
                shift
                ;;
        esac
    done

    if ! command -v "$ADB" >/dev/null 2>&1; then
        echo "adb introuvable. Renseigner ADB=/chemin/vers/adb." >&2
        exit 1
    fi

    local device_count
    device_count=$("$ADB" devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
    if [ "$device_count" != "1" ]; then
        echo "Précondition manquante : exactement un appareil branché est attendu, $device_count trouvé(s)." >&2
        exit 1
    fi

    if ! "$ADB" shell pm list packages | tr -d '\r' | grep -qx "package:$PACKAGE"; then
        echo "Précondition manquante : $PACKAGE n'est pas installé." >&2
        exit 1
    fi

    echo "== Précondition : session armée =="
    echo "  Armer une session dans Niumi (parcours complet : diagnostic, boîtier, applications,"
    echo "  heure de réveil la plus proche possible, sonnerie choisie à l'écran 14, activation),"
    echo "  puis revenir ici."
    printf '  Appuyer sur Entrée une fois la session armée... '
    read -r _

    local target_line
    target_line=$(alarm_block "$WAKE_ALARM_TAG" | head -1)
    if [ -z "$target_line" ]; then
        echo "Aucune alarme de réveil Niumi trouvée dans dumpsys alarm : la session n'est pas armée." >&2
        exit 1
    fi
    echo "Alarme programmée : $target_line"

    # `origWhen` donne l'instant cible en millisecondes depuis l'epoch : c'est la référence du
    # retard, et elle vient du système plutôt que d'un délai saisi par l'opérateur.
    local target_ms
    target_ms=$(alarm_when_ms "$WAKE_ALARM_TAG")
    if [ -z "$target_ms" ]; then
        echo "Instant cible illisible dans dumpsys alarm ; le retard ne pourra pas être mesuré." >&2
    fi

    # Réglage de sonnerie et de montée : la projection Direct Boot fait foi si elle est lisible.
    local ringtone_key="" ramp_seconds="" extras_source="aucune (ni projection, ni options)"
    if read_session_extras; then
        ringtone_key=$(read_json_field "$SESSION_PROJECTION_JSON" ringtoneKey)
        ramp_seconds=$(read_json_field "$SESSION_PROJECTION_JSON" volumeRampSeconds)
        extras_source="projection Direct Boot sur l'appareil"
    elif [ -n "$cli_ringtone" ]; then
        ringtone_key="$cli_ringtone"
        if [ "$cli_ramp_arg" != "none" ]; then
            ramp_seconds="$cli_ramp_arg"
        fi
        extras_source="options --ringtone/--ramp (projection illisible, APK release probable)"
    fi
    echo "Sonnerie et montée lues depuis : $extras_source"
    if [ -n "$ringtone_key" ]; then
        echo "  Sonnerie : $ringtone_key ; montée : ${ramp_seconds:-constante}"
    else
        echo "  Inconnues : la mesure se limite au relevé dumpsys final (ni durée de fichier, ni série d'amplitude)."
    fi

    local file_duration_s=""
    if [ -n "$ringtone_key" ]; then
        file_duration_s=$(wav_duration_seconds "$ringtone_key")
        if [ -z "$file_duration_s" ]; then
            echo "  Durée du fichier illisible pour la clé '$ringtone_key' (clé hors catalogue ou fichier absent du dépôt)."
        else
            echo "  Durée du fichier (dépôt) : ${file_duration_s} s."
        fi
    fi

    local started_at_ms deadline_ms
    started_at_ms=$("$ADB" shell date +%s%3N | tr -d '\r')
    deadline_ms=$((started_at_ms + max_wait * 1000))

    local logfile
    logfile=$(mktemp -t niumi_validate_alarm_logcat)
    # Pas de `logcat -c` : on ne vide jamais le journal de l'opérateur. On ne lit que ce qui est
    # écrit à partir de maintenant en filtrant sur l'horodatage à l'analyse, pas à la capture.
    # Filtré sur les tags AudioTrack/AudioTrackImpl (`-s … *:S`) : sans ce filtre, la première
    # campagne (2026-10-06) a rempli le tampon logcat avec le trafic audio de tout le système sur
    # 2 minutes, au point qu'une capture tardive ne contenait plus les tout premiers `setVolume`
    # de la montée — le tampon avait tourné. Un tampon filtré à la source retient beaucoup plus
    # longtemps les lignes qui comptent.
    "$ADB" logcat -v epoch -d -s AudioTrack:* AudioTrackImpl:* '*:S' >"$logfile" 2>/dev/null || true

    echo "== Attente du déclenchement (limite : ${max_wait} s) =="
    local fired_ms=""
    while :; do
        local now_ms
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

    if [ -z "$fired_ms" ]; then
        echo
        echo "## Résultat"
        echo
        echo "- Service jamais détecté."
        echo "- Logcat capturé (non supprimé, à inspecter si besoin) : $logfile"
        exit 1
    fi

    # Origine de la montée : ringingAtEpochMillis de la projection si elle est de nouveau lisible
    # (elle l'est, le service venant de démarrer), sinon l'instant de détection — moins précis,
    # dit comme tel.
    local ringing_at_ms="$fired_ms" ringing_at_source="détection du service (approximatif, ± 2 s)"
    if read_session_extras; then
        local re_ringing
        re_ringing=$(read_json_field "$SESSION_PROJECTION_JSON" ringingAtEpochMillis)
        if [ -n "$re_ringing" ]; then
            ringing_at_ms="$re_ringing"
            ringing_at_source="ringingAtEpochMillis (projection)"
        fi
    fi

    if [ -n "$file_duration_s" ]; then
        local file_duration_ms
        file_duration_ms=$(awk -v s="$file_duration_s" 'BEGIN { printf "%d", s * 1000 }')
        local ramp_duration_ms=0
        [ -n "$ramp_seconds" ] && ramp_duration_ms=$((ramp_seconds * 1000))

        echo "== Relevés dumpsys programmés (ne pas scanner avant la fin de la mesure) =="
        sample_dumpsys_audio() {
            local label="$1" target_ms="$2" now_ms wait_s
            now_ms=$("$ADB" shell date +%s%3N | tr -d '\r')
            wait_s=$(( (target_ms - now_ms) / 1000 ))
            if [ "$wait_s" -gt 0 ]; then
                sleep "$wait_s"
            fi
            local reading
            reading=$("$ADB" shell dumpsys audio 2>/dev/null | tr -d '\r' | grep -m1 -E "usage=USAGE_ALARM" | sed 's/^[[:space:]]*//')
            echo "  - $label : ${reading:-aucun lecteur USAGE_ALARM trouvé}"
        }
        if [ "$ramp_duration_ms" -gt 0 ]; then
            sample_dumpsys_audio "t0 (départ de la montée)" "$ringing_at_ms"
            sample_dumpsys_audio "t0 + durée/2" "$((ringing_at_ms + ramp_duration_ms / 2))"
            sample_dumpsys_audio "t0 + durée + 5 s" "$((ringing_at_ms + ramp_duration_ms + 5000))"
        fi
        sample_dumpsys_audio "t0 + 3 × durée du fichier (boucle)" "$((ringing_at_ms + 3 * file_duration_ms))"

        echo "== Analyse du logcat capturé depuis le démarrage du script =="
        "$ADB" logcat -v epoch -d -s AudioTrack:* AudioTrackImpl:* '*:S' >>"$logfile" 2>/dev/null || true
        local volume_points
        volume_points=$(analyze_ramp_logcat "$logfile")
        local point_count
        point_count=$(printf '%s\n' "$volume_points" | grep -c . || true)
        if [ "$ramp_duration_ms" -gt 0 ] && [ "$point_count" -ge "$RAMP_MIN_POINTS" ]; then
            echo "  Montée : $point_count valeurs de setVolume reconnues sur la fenêtre."
            local first_line first_ts first_val
            first_line=$(printf '%s\n' "$volume_points" | head -1)
            first_ts=$(awk '{print $1}' <<<"$first_line")
            first_val=$(awk '{print $2}' <<<"$first_line")
            local elapsed_ms_first
            elapsed_ms_first=$(awk -v ts="$first_ts" -v r="$ringing_at_ms" 'BEGIN { printf "%d", ts * 1000 - r }')
            local theory_first
            theory_first=$(amplitude_at "$elapsed_ms_first" "$ramp_duration_ms")
            echo "    première valeur : $first_val à ${elapsed_ms_first} ms écoulés (théorie : $theory_first)"
            local last_line last_val
            last_line=$(printf '%s\n' "$volume_points" | tail -1)
            last_val=$(awk '{print $2}' <<<"$last_line")
            echo "    dernière valeur : $last_val"
        elif [ "$ramp_duration_ms" -gt 0 ]; then
            echo "  Montée : seulement $point_count valeur(s) de setVolume reconnue(s) dans le logcat — pas assez pour conclure."
            echo "  Format non reconnu sur cet appareil, ou journalisation absente. Lignes brutes :"
            grep -i 'audiotrack' "$logfile" 2>/dev/null | tail -20 | sed 's/^/    /' || echo "    (aucune ligne AudioTrack trouvée)"
        fi

        local loop_events
        loop_events=$(analyze_loop_logcat "$logfile")
        local loop_count
        loop_count=$(printf '%s\n' "$loop_events" | grep -c . || true)
        if [ "$loop_count" -ge 2 ]; then
            echo "  Boucle : $loop_count évènements start/stop/pause/flush reconnus — période à comparer manuellement à ${file_duration_s} s."
        else
            echo "  Boucle : pas assez d'évènements reconnus pour mesurer la période des reprises."
        fi
    fi
    echo "Logcat capturé (non supprimé, à inspecter si besoin) : $logfile"

    echo
    echo "## Résultat"
    echo
    if [ -n "$target_ms" ]; then
        # Le sondage a un pas de 2 s : le retard mesuré est donc précis à ± 2 s près.
        local delay=$(( (fired_ms - target_ms) / 1000 ))
        echo "- Service détecté au premier plan ${delay} s après l'instant cible (sondage à ± 2 s)."
    else
        echo "- Service détecté au premier plan, retard non mesurable (instant cible illisible)."
    fi
    echo "- Service : $("$ADB" shell dumpsys activity services "$PACKAGE" 2>/dev/null | tr -d '\r' | grep -m1 -E "isForeground|ServiceRecord.*AlarmRingingService" | sed 's/^[[:space:]]*//')"
    echo "- Lecteur audio : $("$ADB" shell dumpsys audio 2>/dev/null | tr -d '\r' | grep -m1 -E "usage=USAGE_ALARM" | sed 's/^[[:space:]]*//')"
    echo "- Flux alarme muté : $("$ADB" shell dumpsys audio 2>/dev/null | tr -d '\r' | sed -n '/^- STREAM_ALARM:/,/^$/p' | grep -m1 "Muted:")"
    echo "- Volume alarme : $("$ADB" shell cmd audio get-stream-volume 4 2>/dev/null | tr -d '\r' | sed 's/.*-> //')"
    echo "- Notification de sonnerie : $("$ADB" shell dumpsys notification --noredact 2>/dev/null | tr -d '\r' | grep -m1 -A3 "pkg=$PACKAGE" | grep -m1 -A2 "RINGING\|alarm" | tr '\n' ' ')"
    echo "- Activité au premier plan : $("$ADB" shell dumpsys activity activities 2>/dev/null | tr -d '\r' | grep -m1 "topResumedActivity" | sed 's/^[[:space:]]*//')"
    echo
    echo "À confirmer à l'oreille : le son est-il réellement audible, la montée perçue comme"
    echo "progressive, la boucle sans blanc ni clic ? Puis scanner le boîtier associé pour"
    echo "terminer la session (aucune action d'arrêt logicielle n'existe, §3)."
}

# Garde : ce fichier peut être sourcé (par un harnais de test) sans jamais lancer main(), qui
# exige adb et une session armée à la main. Seule une exécution directe (`./validate_alarm.sh`
# ou `bash validate_alarm.sh`) déclenche le protocole interactif.
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi
