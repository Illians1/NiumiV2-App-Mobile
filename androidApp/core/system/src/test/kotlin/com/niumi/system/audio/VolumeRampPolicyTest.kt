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

    @Test
    fun durationsAreTheClosedListWithTwoMinutesByDefault() {
        assertThat(VolumeRampDurations.SECONDS).containsExactly(30, 60, 120, 300).inOrder()
        assertThat(VolumeRampDurations.DEFAULT_SECONDS).isEqualTo(120)
        assertThat(VolumeRampDurations.SECONDS).contains(VolumeRampDurations.DEFAULT_SECONDS)
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
