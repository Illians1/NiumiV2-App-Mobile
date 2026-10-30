package com.niumi.system.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.niumi.system.common.Clock

/**
 * Vibration répétée pendant la sonnerie (SPEC_ANDROID §10.2), `minSdk 29`. Traducteur mince
 * vers `VibrationEffect` (stub non testable en JVM) : la décision du motif à jouer, y compris
 * la progression qui suit la montée du volume et la reprise après une vibration d'erreur, vit
 * dans [VibrationPatternPolicy], testable. Le motif est recalculé au temps écoulé lu sur [clock]
 * à chaque `vibrate()` : une reprise après erreur repart au rythme atteint, pas du début.
 */
class AndroidVibrationController(
    private val context: Context,
    private val clock: Clock,
) : VibrationController {
    private val vibrator: Vibrator
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

    /** Vibration d'alarme en cours, avec sa montée éventuelle ; `null` si aucune. */
    private class Ongoing(
        val ramp: VolumeRamp?,
    )

    @Volatile
    private var ongoing: Ongoing? = null

    override fun startRepeating(ramp: VolumeRamp?) {
        val started = Ongoing(ramp)
        ongoing = started
        play(patternFor(started))
    }

    override fun vibrateError() {
        play(VibrationPatternPolicy.error(resume = ongoing?.let(::patternFor)))
    }

    override fun stop() {
        ongoing = null
        vibrator.cancel()
    }

    private fun patternFor(current: Ongoing): VibrationPattern {
        val ramp = current.ramp ?: return VibrationPatternPolicy.repeating()
        return VibrationPatternPolicy.repeating(
            elapsedMs = clock.nowEpochMillis() - ramp.startedAtEpochMillis,
            durationMs = ramp.durationMs,
        )
    }

    private fun play(pattern: VibrationPattern) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, pattern.repeatFromIndex))
    }
}
