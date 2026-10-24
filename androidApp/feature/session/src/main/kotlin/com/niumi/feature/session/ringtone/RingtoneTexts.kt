package com.niumi.feature.session.ringtone

import com.niumi.feature.session.ui.AlarmSoundTexts
import com.niumi.system.audio.VolumeRampDurations

/**
 * Textes de la feuille « Sonnerie » (`SPECIFICATION.md` §10, maquettes 16 et 17, étape 40),
 * tutoiement partout. Les quatre derniers messages ([NOT_ARMED_MESSAGE], [PREVIEW_FAILED],
 * [SAVE_FAILED], [SESSION_UPDATE_FAILED]) couvrent des échecs que la spec ne nomme pas : repris
 * tels quels de l'écran 14 (Lot 7, étape 27, faiblesse de spec déjà signalée) — sans eux la feuille
 * laisserait croire qu'un geste a réussi quand il n'a rien fait (§15, ne jamais afficher un faux
 * état de fiabilité).
 */
object RingtoneTexts {
    const val TITLE = "Sonnerie"
    const val CLOSE = "Fermer"

    const val SOUND_SECTION = "Son"
    const val SOUND_GROUP_DESCRIPTION = "Son de la sonnerie"

    /** Libellé de la ligne à interrupteur (maquette 18, 2026-10-23) ; le même mot décrit l'interrupteur. */
    const val RAMP_SECTION = "Sonnerie progressive"
    const val RAMP_SWITCH_DESCRIPTION = "Sonnerie progressive"
    const val RAMP_GROUP_DESCRIPTION = "Sonnerie progressive : durée pour atteindre le volume maximal"

    /** Libellés des quatre segments de la barre, dans l'ordre de [VolumeRampDurations.SECONDS] (Q7). */
    val RAMP_LABELS: List<String> = VolumeRampDurations.SECONDS.map(AlarmSoundTexts::durationLabel)

    const val PHONE_VOLUME = "Volume du téléphone"
    const val VOLUME_HINT = "Il se règle dans les réglages de ton téléphone."

    const val ALARM_VOLUME_ZERO = "Le volume des alarmes est à zéro. Monte-le pour entendre la pré-écoute."

    const val STOP_PREVIEW_DESCRIPTION = "Arrêter l’écoute"

    /** La session a quitté `ARMED` entre l'ouverture de la feuille et le changement (§15). */
    const val NOT_ARMED_MESSAGE = "Le réveil a déjà commencé : ce choix s’appliquera à ta prochaine session."

    const val PREVIEW_FAILED = "La pré-écoute n’a pas pu démarrer."
    const val SAVE_FAILED = "Ton choix n’a pas pu être enregistré."
    const val SESSION_UPDATE_FAILED = "Ce choix n’a pas pu être appliqué au réveil déjà programmé."

    /**
     * Sous-ligne de l'interrupteur, absente quand il est éteint (D6, 2026-10-23). Texte revu le
     * 2026-10-24 (« Volume max en… » auparavant) : il nomme le volume du téléphone, affiché juste au-dessus.
     */
    fun rampMention(seconds: Int): String =
        "Monte jusqu’au volume du téléphone en ${AlarmSoundTexts.durationLabel(seconds)}"

    fun previewDescription(label: String): String = "Écouter $label"

    fun volumeDescription(percent: Int): String = "$PHONE_VOLUME : $percent %"
}
