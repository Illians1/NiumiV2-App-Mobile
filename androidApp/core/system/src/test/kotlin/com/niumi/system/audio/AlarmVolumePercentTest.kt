package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pourcentage affiché dans la feuille « Sonnerie » (SPECIFICATION.md §10, étape 40) : fonction pure
 * pour rester testable en JVM, `AndroidAlarmVolumeSource` ne faisant qu'y passer les deux valeurs
 * lues sur `AudioManager`.
 */
class AlarmVolumePercentTest {
    @Test
    fun roundsToTheNearestPercent() {
        assertThat(alarmVolumePercent(volume = 7, max = 15)).isEqualTo(47)
    }

    @Test
    fun maximumVolumeIsAHundredPercent() {
        assertThat(alarmVolumePercent(volume = 15, max = 15)).isEqualTo(100)
    }

    @Test
    fun zeroVolumeIsZeroPercent() {
        assertThat(alarmVolumePercent(volume = 0, max = 15)).isEqualTo(0)
    }

    @Test
    fun aNonPositiveMaximumIsUnreadable() {
        assertThat(alarmVolumePercent(volume = 0, max = 0)).isNull()
        assertThat(alarmVolumePercent(volume = 1, max = -1)).isNull()
    }
}
