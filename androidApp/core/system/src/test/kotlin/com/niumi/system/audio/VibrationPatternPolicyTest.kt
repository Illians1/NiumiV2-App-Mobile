package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §11.2 : une vibration courte d'erreur (tag non associé) ne doit jamais couper
 * la vibration d'alarme en cours (§10.2) — `Vibrator.vibrate()` remplaçant tout effet actif,
 * la reprise doit être encodée dans le motif lui-même plutôt que rappelée après coup.
 * Descripteur pur (`VibrationEffect` est un stub Android non testable en JVM) : voir
 * `AndroidVibrationController`.
 */
class VibrationPatternPolicyTest {
    @Test
    fun repeatingPatternLoopsFromTheFirstPulse() {
        val pattern = VibrationPatternPolicy.repeating()

        assertThat(pattern.timings).isEqualTo(longArrayOf(0, 500, 500))
        assertThat(pattern.repeatFromIndex).isEqualTo(0)
    }

    @Test
    fun errorPatternWithoutOngoingRepetitionDoesNotLoop() {
        val pattern = VibrationPatternPolicy.error(wasRepeating = false)

        assertThat(pattern.timings).isEqualTo(longArrayOf(0, 200))
        assertThat(pattern.repeatFromIndex).isEqualTo(-1)
    }

    @Test
    fun errorPatternDuringOngoingRepetitionResumesTheAlarmPattern() {
        val pattern = VibrationPatternPolicy.error(wasRepeating = true)

        assertThat(pattern.timings).isEqualTo(longArrayOf(0, 200, 300, 500, 500))
        assertThat(pattern.repeatFromIndex).isEqualTo(3)
    }
}
