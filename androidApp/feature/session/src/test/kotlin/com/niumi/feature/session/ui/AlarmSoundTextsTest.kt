package com.niumi.feature.session.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.system.audio.AlarmSoundSettings
import org.junit.Test

/**
 * Résumé partagé des écrans 5, 6 et 7 (SPECIFICATION.md §9, maquette 16, étape 40) : le nom du son
 * et, dessous, « Progressive · {durée} » — absent quand la sonnerie progressive est sur « Non »
 * (D5, D6, tranchés le 2026-10-23). Les durées s'écrivent « 2 min », « 5 min », « 10 min »,
 * « 15 min » (resserré le 2026-10-08, durées reprises telles quelles à l'étape 40, Q7). Mot pour
 * mot, verrouillé ici.
 */
class AlarmSoundTextsTest {
    @Test
    fun durationLabelsMatchTheSpecExactly() {
        assertThat(AlarmSoundTexts.durationLabel(120)).isEqualTo("2 min")
        assertThat(AlarmSoundTexts.durationLabel(300)).isEqualTo("5 min")
        assertThat(AlarmSoundTexts.durationLabel(600)).isEqualTo("10 min")
        assertThat(AlarmSoundTexts.durationLabel(900)).isEqualTo("15 min")
    }

    @Test
    fun aConstantVolumeHasNoDetail() {
        val summary = AlarmSoundTexts.summary(AlarmSoundSettings("niumi_bell", null))

        assertThat(summary.name).isEqualTo("Cloche")
        assertThat(summary.detail).isNull()
    }

    /** Défaut 10 min depuis le 2026-10-23 (révision de l'étape 40). */
    @Test
    fun theDefaultSettingsSummarizeAsPianoWithATenMinuteRamp() {
        val summary = AlarmSoundTexts.summary(AlarmSoundSettings())

        assertThat(summary.name).isEqualTo("Piano")
        assertThat(summary.detail).isEqualTo("Progressive · 10 min")
    }

    @Test
    fun otherRingtonesAndDurationsAreSummarizedTheSameWay() {
        val summary = AlarmSoundTexts.summary(AlarmSoundSettings("niumi_oiseaux", 300))

        assertThat(summary.name).isEqualTo("Oiseaux")
        assertThat(summary.detail).isEqualTo("Progressive · 5 min")
    }

    /**
     * Une clé hors catalogue ne doit jamais faire planter l'affichage : même principe que
     * `RingingSoundResolver`, on replie sur le libellé par défaut plutôt que d'afficher du vide.
     */
    @Test
    fun anUnknownRingtoneKeyFallsBackToTheDefaultLabel() {
        val summary = AlarmSoundTexts.summary(AlarmSoundSettings("niumi_alarm", null))

        assertThat(summary.name).isEqualTo("Piano")
        assertThat(summary.detail).isNull()
    }

    /**
     * `summary` assainit lui-même ses réglages (étape 40) : une session déjà armée avec une durée
     * hors liste (900 avant l'étape 28 n'existait pas encore, mais 30 ou 60 existaient) ne doit
     * jamais faire planter `durationLabel` — avant cette étape, `ActiveSessionViewModel` ne
     * passait pas par `sanitized()` avant d'appeler `summary`.
     */
    @Test
    fun aRampDurationOutsideTheClosedListIsSanitizedBeforeSummarizing() {
        val summary = AlarmSoundTexts.summary(AlarmSoundSettings("niumi_piano", 60))

        assertThat(summary.detail).isEqualTo("Progressive · 2 min")
    }
}
