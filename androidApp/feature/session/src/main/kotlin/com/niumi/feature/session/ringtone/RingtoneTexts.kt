package com.niumi.feature.session.ringtone

import com.niumi.feature.session.ui.AlarmSoundTexts

/**
 * Textes de l'écran 14 (SPEC_ANDROID §15, Lot 7), tutoiement partout. Les trois derniers
 * ([PREVIEW_FAILED], [SAVE_FAILED], [SESSION_UPDATE_FAILED]) couvrent des échecs que la spec ne
 * nomme pas : décision prise à l'étape 27 (faiblesse de spec signalée, voir `ETAPE-27.md`), sans
 * eux l'écran laisserait croire qu'un geste a réussi quand il n'a rien fait (§15, ne jamais
 * afficher un faux état de fiabilité).
 */
object RingtoneTexts {
    const val TITLE = "Sonnerie"

    const val VOLUME_SECTION_TITLE = "Volume"

    const val RAMP_SWITCH_LABEL = "Volume progressif"

    const val STOP_PREVIEW_DESCRIPTION = "Arrêter l'écoute"

    const val ALARM_VOLUME_ZERO = "Le volume des alarmes est à zéro. Monte-le pour entendre la pré-écoute."

    /** Écran 14 ouvert depuis l'écran 7 en `ARMED` (§15, dérogation « sans scan »). */
    const val ARMED_BANNER = "Ce choix s'applique aussi au réveil déjà programmé."

    /** La session a quitté `ARMED` entre l'ouverture de l'écran et le changement (§15). */
    const val NOT_ARMED_MESSAGE = "Le réveil a déjà commencé : ce choix s'appliquera à ta prochaine session."

    const val PREVIEW_FAILED = "La pré-écoute n'a pas pu démarrer."

    const val SAVE_FAILED = "Ton choix n'a pas pu être enregistré."

    const val SESSION_UPDATE_FAILED = "Ce choix n'a pas pu être appliqué au réveil déjà programmé."

    fun rampDescription(seconds: Int): String =
        "L'alarme démarre doucement et atteint son volume maximal en ${AlarmSoundTexts.durationLabel(seconds)}."

    fun previewDescription(label: String): String = "Écouter $label"
}
