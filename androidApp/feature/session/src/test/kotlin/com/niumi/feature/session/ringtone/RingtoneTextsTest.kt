package com.niumi.feature.session.ringtone

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Textes de l'écran 14 (SPEC_ANDROID §15, Lot 7), tutoiement partout, mot pour mot. */
class RingtoneTextsTest {
    private val allTexts =
        listOf(
            RingtoneTexts.TITLE,
            RingtoneTexts.VOLUME_SECTION_TITLE,
            RingtoneTexts.RAMP_SWITCH_LABEL,
            RingtoneTexts.STOP_PREVIEW_DESCRIPTION,
            RingtoneTexts.ALARM_VOLUME_ZERO,
            RingtoneTexts.ARMED_BANNER,
            RingtoneTexts.NOT_ARMED_MESSAGE,
            RingtoneTexts.PREVIEW_FAILED,
            RingtoneTexts.SAVE_FAILED,
            RingtoneTexts.SESSION_UPDATE_FAILED,
            RingtoneTexts.rampDescription(120),
            RingtoneTexts.previewDescription("Piano"),
        )

    @Test
    fun noTextUsesVouvoiement() {
        allTexts.forEach { text ->
            assertThat(text).doesNotContain("vous")
            assertThat(text).doesNotContain("votre")
            assertThat(text).doesNotContain("Vous")
            assertThat(text).doesNotContain("Votre")
        }
    }

    @Test
    fun theSpecTextsAreQuotedWordForWord() {
        assertThat(RingtoneTexts.TITLE).isEqualTo("Sonnerie")
        assertThat(RingtoneTexts.VOLUME_SECTION_TITLE).isEqualTo("Volume")
        assertThat(RingtoneTexts.RAMP_SWITCH_LABEL).isEqualTo("Volume progressif")
        assertThat(RingtoneTexts.ALARM_VOLUME_ZERO)
            .isEqualTo("Le volume des alarmes est à zéro. Monte-le pour entendre la pré-écoute.")
        assertThat(RingtoneTexts.ARMED_BANNER).isEqualTo("Ce choix s'applique aussi au réveil déjà programmé.")
        assertThat(RingtoneTexts.NOT_ARMED_MESSAGE)
            .isEqualTo("Le réveil a déjà commencé : ce choix s'appliquera à ta prochaine session.")
        assertThat(RingtoneTexts.STOP_PREVIEW_DESCRIPTION).isEqualTo("Arrêter l'écoute")
    }

    @Test
    fun theRampDescriptionNamesTheChosenDuration() {
        assertThat(RingtoneTexts.rampDescription(120))
            .isEqualTo("L'alarme démarre doucement et atteint son volume maximal en 2 min.")
        assertThat(RingtoneTexts.rampDescription(30))
            .isEqualTo("L'alarme démarre doucement et atteint son volume maximal en 30 s.")
    }

    @Test
    fun thePreviewDescriptionNamesTheRingtone() {
        assertThat(RingtoneTexts.previewDescription("Piano")).isEqualTo("Écouter Piano")
    }
}
