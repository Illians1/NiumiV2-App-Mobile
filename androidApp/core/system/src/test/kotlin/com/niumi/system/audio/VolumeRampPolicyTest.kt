package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** SPEC_ANDROID §10.2 : courbe `10^(−2·(1−p))`, de −40 dB au départ à 0 dB à la durée. */
class VolumeRampPolicyTest {
    @Test
    fun startsAtMinusFortyDecibels() {
        assertThat(VolumeRampPolicy.amplitudeAt(0, MINUTE)).isEqualTo(VolumeRampPolicy.START_AMPLITUDE)
        assertThat(VolumeRampPolicy.START_AMPLITUDE).isEqualTo(0.01f)
    }

    @Test
    fun reachesFullVolumeAtTheDurationAndStaysThere() {
        assertThat(VolumeRampPolicy.amplitudeAt(MINUTE, MINUTE)).isEqualTo(1f)
        assertThat(VolumeRampPolicy.amplitudeAt(90_000, MINUTE)).isEqualTo(1f)
    }

    @Test
    fun negativeElapsedTimeIsClampedToTheStart() {
        assertThat(VolumeRampPolicy.amplitudeAt(-5_000, MINUTE)).isEqualTo(0.01f)
    }

    @Test
    fun halfwayIsMinusTwentyDecibels() {
        assertThat(VolumeRampPolicy.amplitudeAt(30_000, MINUTE)).isWithin(1e-4f).of(0.1f)
    }

    @Test
    fun strictlyIncreasingOverEveryTick() {
        val amplitudes =
            (0..MINUTE / VolumeRampPolicy.TICK_MS).map {
                VolumeRampPolicy.amplitudeAt(it * VolumeRampPolicy.TICK_MS, MINUTE)
            }

        assertThat(amplitudes).hasSize(241)
        amplitudes.zipWithNext().forEach { (previous, next) -> assertThat(next).isGreaterThan(previous) }
    }

    @Test
    fun aNonPositiveDurationMeansFullVolume() {
        assertThat(VolumeRampPolicy.amplitudeAt(0, 0)).isEqualTo(1f)
        assertThat(VolumeRampPolicy.amplitudeAt(10, -1)).isEqualTo(1f)
    }

    @Test
    fun isCompleteFromTheDurationOn() {
        assertThat(VolumeRampPolicy.isComplete(MINUTE - 1, MINUTE)).isFalse()
        assertThat(VolumeRampPolicy.isComplete(MINUTE, MINUTE)).isTrue()
        assertThat(VolumeRampPolicy.isComplete(MINUTE + 1, MINUTE)).isTrue()
        assertThat(VolumeRampPolicy.isComplete(0, 0)).isTrue()
    }

    /** Défaut 10 min depuis le 2026-10-23 (révision de l'étape 40), 2 min du 2026-09-30 à cette date. */
    @Test
    fun durationsAreTheClosedListWithTenMinutesByDefault() {
        assertThat(VolumeRampDurations.SECONDS).containsExactly(120, 300, 600, 900).inOrder()
        assertThat(VolumeRampDurations.DEFAULT_SECONDS).isEqualTo(600)
        assertThat(VolumeRampDurations.SECONDS).contains(VolumeRampDurations.DEFAULT_SECONDS)
    }

    @Test
    fun sanitizedKeepsNullAndAnyValueAlreadyInTheList() {
        assertThat(VolumeRampDurations.sanitized(null)).isNull()
        assertThat(VolumeRampDurations.sanitized(120)).isEqualTo(120)
        assertThat(VolumeRampDurations.sanitized(900)).isEqualTo(900)
    }

    @Test
    fun sanitizedRoundsAnOutsideValueToTheClosestDuration() {
        // Durées d'avant l'étape 28 (30 s, 1 min), reprises aussi par AlarmSoundSettingsTest.
        assertThat(VolumeRampDurations.sanitized(30)).isEqualTo(120)
        assertThat(VolumeRampDurations.sanitized(60)).isEqualTo(120)
        assertThat(VolumeRampDurations.sanitized(1_000)).isEqualTo(900)
    }

    @Test
    fun sanitizedBreaksATieTowardsTheShorterDuration() {
        // 450 est à 150 de 300 comme de 600 ; le plein volume arrive plus tôt avec 300.
        assertThat(VolumeRampDurations.sanitized(450)).isEqualTo(300)
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
