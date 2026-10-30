package com.niumi.system.audio

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §10.2 et §11.2. Une vibration courte d'erreur (tag non associé) ne doit jamais
 * couper la vibration d'alarme en cours — `Vibrator.vibrate()` remplaçant tout effet actif,
 * la reprise doit être encodée dans le motif lui-même plutôt que rappelée après coup. Avec une
 * montée progressive, le rythme des impulsions se resserre sur la même durée que le volume
 * (décision du 2026-10-26). Descripteur pur (`VibrationEffect` est un stub Android non testable
 * en JVM) : voir `AndroidVibrationController`.
 */
class VibrationPatternPolicyTest {
    private val twoMinutes = 120_000L

    @Test
    fun repeatingPatternLoopsOnTheFullRhythmPulse() {
        val pattern = VibrationPatternPolicy.repeating()

        assertThat(pattern.timings).isEqualTo(longArrayOf(0, 800, 2_200))
        assertThat(pattern.repeatFromIndex).isEqualTo(1)
    }

    @Test
    fun withoutRampDurationTheRhythmIsFullFromTheStart() {
        assertThat(VibrationPatternPolicy.repeating(elapsedMs = 0, durationMs = 0))
            .isEqualTo(VibrationPatternPolicy.repeating())
    }

    @Test
    fun onceTheRampIsCompleteTheRhythmIsFull() {
        assertThat(VibrationPatternPolicy.repeating(elapsedMs = twoMinutes, durationMs = twoMinutes))
            .isEqualTo(VibrationPatternPolicy.repeating())
        assertThat(VibrationPatternPolicy.repeating(elapsedMs = twoMinutes + 5_000, durationMs = twoMinutes))
            .isEqualTo(VibrationPatternPolicy.repeating())
    }

    @Test
    fun theRampStartsWithOnePulseEverySixteenSeconds() {
        val timings = VibrationPatternPolicy.repeating(elapsedMs = 0, durationMs = twoMinutes).timings

        assertThat(timings[0]).isEqualTo(0)
        assertThat(timings[1]).isEqualTo(800)
        assertThat(timings[2]).isEqualTo(15_500)
    }

    @Test
    fun pausesShrinkStrictlyAndPulsesStayConstant() {
        val pattern = VibrationPatternPolicy.repeating(elapsedMs = 0, durationMs = twoMinutes)
        val rampTimings = pattern.timings.copyOfRange(1, pattern.repeatFromIndex)
        val pulses = rampTimings.filterIndexed { index, _ -> index % 2 == 0 }
        val pauses = rampTimings.filterIndexed { index, _ -> index % 2 == 1 }

        assertThat(pulses.toSet()).containsExactly(800L)
        assertThat(pauses).isInStrictOrder(Comparator.reverseOrder<Long>())
        assertThat(pauses.last()).isAtLeast(2_200L)
    }

    @Test
    fun theRampEndsOnTheFullRhythmLoop() {
        val pattern = VibrationPatternPolicy.repeating(elapsedMs = 0, durationMs = twoMinutes)
        val loop = pattern.timings.copyOfRange(pattern.repeatFromIndex, pattern.timings.size)

        assertThat(loop).isEqualTo(longArrayOf(800, 2_200))
    }

    @Test
    fun theRampCoversTheWholeRemainingDuration() {
        val pattern = VibrationPatternPolicy.repeating(elapsedMs = 30_000, durationMs = twoMinutes)
        val beforeLoop = pattern.timings.copyOfRange(0, pattern.repeatFromIndex).sum()

        assertThat(beforeLoop).isAtLeast(twoMinutes - 30_000)
    }

    @Test
    fun theRampPauseHalfwayIsTheGeometricMean() {
        assertThat(VibrationPatternPolicy.pauseAt(elapsedMs = 60_000, durationMs = twoMinutes))
            .isIn(Range.closed(5_839L, 5_841L))
    }

    @Test
    fun pauseIsBoundedOutsideTheRamp() {
        assertThat(VibrationPatternPolicy.pauseAt(elapsedMs = -1_000, durationMs = twoMinutes)).isEqualTo(15_500)
        assertThat(VibrationPatternPolicy.pauseAt(elapsedMs = twoMinutes * 2, durationMs = twoMinutes)).isEqualTo(2_200)
    }

    @Test
    fun aResumedRampStartsAtTheRhythmAlreadyReached() {
        val timings = VibrationPatternPolicy.repeating(elapsedMs = 60_000, durationMs = twoMinutes).timings

        assertThat(timings[1]).isEqualTo(800)
        assertThat(timings[2]).isEqualTo(VibrationPatternPolicy.pauseAt(60_000, twoMinutes))
    }

    @Test
    fun aNegativeElapsedTimeStartsAtTheBeginning() {
        assertThat(VibrationPatternPolicy.repeating(elapsedMs = -5_000, durationMs = twoMinutes))
            .isEqualTo(VibrationPatternPolicy.repeating(elapsedMs = 0, durationMs = twoMinutes))
    }

    @Test
    fun theLongestRampNeverExceedsOnePulsePerSecond() {
        val fifteenMinutes = 900_000L
        val pattern = VibrationPatternPolicy.repeating(elapsedMs = 0, durationMs = fifteenMinutes)
        val rampPulses = (pattern.repeatFromIndex - 1) / 2

        assertThat(rampPulses).isAtMost((fifteenMinutes / 1_000).toInt())
    }

    @Test
    fun errorPatternWithoutOngoingRepetitionDoesNotLoop() {
        val pattern = VibrationPatternPolicy.error(resume = null)

        assertThat(pattern.timings).isEqualTo(longArrayOf(0, 200))
        assertThat(pattern.repeatFromIndex).isEqualTo(-1)
    }

    @Test
    fun errorPatternDuringOngoingRepetitionResumesTheAlarmPattern() {
        val pattern = VibrationPatternPolicy.error(resume = VibrationPatternPolicy.repeating())

        assertThat(pattern.timings).isEqualTo(longArrayOf(0, 200, 300, 800, 2_200))
        assertThat(pattern.repeatFromIndex).isEqualTo(3)
    }

    @Test
    fun errorPatternDuringTheRampResumesAtTheRhythmReached() {
        val resume = VibrationPatternPolicy.repeating(elapsedMs = 60_000, durationMs = twoMinutes)
        val pattern = VibrationPatternPolicy.error(resume = resume)

        assertThat(pattern.timings.copyOfRange(0, 5))
            .isEqualTo(longArrayOf(0, 200, 300, 800, resume.timings[2]))
        assertThat(pattern.timings.copyOfRange(3, pattern.timings.size))
            .isEqualTo(resume.timings.copyOfRange(1, resume.timings.size))
        assertThat(pattern.repeatFromIndex).isEqualTo(resume.repeatFromIndex + 2)
    }
}
