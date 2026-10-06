package com.niumi.feature.session.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.system.audio.AlarmSoundSettings
import org.junit.Test

/**
 * Résumé partagé des écrans 5, 6 et 7 (SPEC_ANDROID §15, Lot 7) : « {Sonnerie} · volume constant »
 * ou « {Sonnerie} · volume progressif sur {durée} », les durées s'écrivant « 30 s », « 1 min »,
 * « 2 min », « 5 min ». Mot pour mot, verrouillé ici.
 */
class AlarmSoundTextsTest {
    @Test
    fun durationLabelsMatchTheSpecExactly() {
        assertThat(AlarmSoundTexts.durationLabel(30)).isEqualTo("30 s")
        assertThat(AlarmSoundTexts.durationLabel(60)).isEqualTo("1 min")
        assertThat(AlarmSoundTexts.durationLabel(120)).isEqualTo("2 min")
        assertThat(AlarmSoundTexts.durationLabel(300)).isEqualTo("5 min")
    }

    @Test
    fun aConstantVolumeIsSummarizedWithoutADuration() {
        assertThat(AlarmSoundTexts.summary(AlarmSoundSettings("niumi_bell", null)))
            .isEqualTo("Cloche · volume constant")
    }

    @Test
    fun theDefaultSettingsSummarizeAsPianoWithATwoMinuteRamp() {
        assertThat(AlarmSoundTexts.summary(AlarmSoundSettings()))
            .isEqualTo("Piano · volume progressif sur 2 min")
    }

    @Test
    fun otherRingtonesAndDurationsAreSummarizedTheSameWay() {
        assertThat(AlarmSoundTexts.summary(AlarmSoundSettings("niumi_oiseaux", 300)))
            .isEqualTo("Oiseaux · volume progressif sur 5 min")
    }

    /**
     * Une clé hors catalogue ne doit jamais faire planter l'affichage : même principe que
     * `RingingSoundResolver`, on replie sur le libellé par défaut plutôt que d'afficher du vide.
     */
    @Test
    fun anUnknownRingtoneKeyFallsBackToTheDefaultLabel() {
        assertThat(AlarmSoundTexts.summary(AlarmSoundSettings("niumi_alarm", null)))
            .isEqualTo("Piano · volume constant")
    }
}
