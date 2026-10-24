package com.niumi.feature.session.ringtone

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Textes de la feuille « Sonnerie » (`SPECIFICATION.md` §10, maquettes 16 et 17, étape 40),
 * tutoiement partout, mot pour mot, apostrophe typographique (D13).
 */
class RingtoneTextsTest {
    @Test
    fun theSpecTextsAreQuotedWordForWord() {
        assertThat(RingtoneTexts.TITLE).isEqualTo("Sonnerie")
        assertThat(RingtoneTexts.CLOSE).isEqualTo("Fermer")
        assertThat(RingtoneTexts.SOUND_SECTION).isEqualTo("Son")
        assertThat(RingtoneTexts.SOUND_GROUP_DESCRIPTION).isEqualTo("Son de la sonnerie")
        assertThat(RingtoneTexts.RAMP_SECTION).isEqualTo("Sonnerie progressive")
        assertThat(RingtoneTexts.RAMP_SWITCH_DESCRIPTION).isEqualTo("Sonnerie progressive")
        assertThat(RingtoneTexts.RAMP_GROUP_DESCRIPTION)
            .isEqualTo("Sonnerie progressive : durée pour atteindre le volume maximal")
        assertThat(RingtoneTexts.PHONE_VOLUME).isEqualTo("Volume du téléphone")
        assertThat(RingtoneTexts.VOLUME_HINT).isEqualTo("Il se règle dans les réglages de ton téléphone.")
        assertThat(RingtoneTexts.ALARM_VOLUME_ZERO)
            .isEqualTo("Le volume des alarmes est à zéro. Monte-le pour entendre la pré-écoute.")
    }

    /** Quatre segments depuis la révision du 2026-10-23 (maquette 18) : « Non » est devenu l'interrupteur. */
    @Test
    fun theDurationSegmentLabelsMatchTheSpecExactly() {
        assertThat(RingtoneTexts.RAMP_LABELS).containsExactly("2 min", "5 min", "10 min", "15 min").inOrder()
    }

    @Test
    fun theRampMentionNamesTheChosenDuration() {
        assertThat(RingtoneTexts.rampMention(120)).isEqualTo("Monte jusqu’au volume du téléphone en 2 min")
        assertThat(RingtoneTexts.rampMention(900)).isEqualTo("Monte jusqu’au volume du téléphone en 15 min")
    }

    @Test
    fun thePreviewDescriptionsNameTheRingtone() {
        assertThat(RingtoneTexts.previewDescription("Piano")).isEqualTo("Écouter Piano")
        assertThat(RingtoneTexts.STOP_PREVIEW_DESCRIPTION).isEqualTo("Arrêter l’écoute")
    }

    @Test
    fun theVolumeDescriptionNamesThePercent() {
        assertThat(RingtoneTexts.volumeDescription(80)).isEqualTo("Volume du téléphone : 80 %")
    }

    @Test
    fun theMessagesNotWrittenByTheMockupAreQuotedWordForWord() {
        assertThat(RingtoneTexts.NOT_ARMED_MESSAGE)
            .isEqualTo("Le réveil a déjà commencé : ce choix s’appliquera à ton prochain réveil.")
        assertThat(RingtoneTexts.PREVIEW_FAILED).isEqualTo("La pré-écoute n’a pas pu démarrer.")
        assertThat(RingtoneTexts.SAVE_FAILED).isEqualTo("Ton choix n’a pas pu être enregistré.")
        assertThat(RingtoneTexts.SESSION_UPDATE_FAILED)
            .isEqualTo("Ce choix n’a pas pu être appliqué au réveil déjà programmé.")
    }

    /** D13 : l'apostrophe droite se glisse par recopie ; elle n'a pas sa place dans l'app. */
    @Test
    fun noTextCarriesAStraightApostrophe() {
        assertThat(allTexts.filter { it.contains('\'') }).isEmpty()
    }

    /** Tutoiement partout (`SPECIFICATION.md` §2). */
    @Test
    fun noTextSwitchesToTheFormalAddress() {
        assertThat(allTexts.filter { FORMAL_ADDRESS.containsMatchIn(it) }).isEmpty()
    }

    private companion object {
        val FORMAL_ADDRESS = Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""")

        val allTexts =
            listOf(
                RingtoneTexts.TITLE,
                RingtoneTexts.CLOSE,
                RingtoneTexts.SOUND_SECTION,
                RingtoneTexts.SOUND_GROUP_DESCRIPTION,
                RingtoneTexts.RAMP_SECTION,
                RingtoneTexts.RAMP_SWITCH_DESCRIPTION,
                RingtoneTexts.RAMP_GROUP_DESCRIPTION,
                RingtoneTexts.PHONE_VOLUME,
                RingtoneTexts.VOLUME_HINT,
                RingtoneTexts.ALARM_VOLUME_ZERO,
                RingtoneTexts.NOT_ARMED_MESSAGE,
                RingtoneTexts.PREVIEW_FAILED,
                RingtoneTexts.SAVE_FAILED,
                RingtoneTexts.SESSION_UPDATE_FAILED,
                RingtoneTexts.STOP_PREVIEW_DESCRIPTION,
                RingtoneTexts.rampMention(120),
                RingtoneTexts.previewDescription("Piano"),
                RingtoneTexts.volumeDescription(80),
            )
    }
}
