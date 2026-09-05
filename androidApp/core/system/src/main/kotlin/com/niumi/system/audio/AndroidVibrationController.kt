package com.niumi.system.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibration répétée pendant la sonnerie (SPEC_ANDROID §10.2), `minSdk 29`. Traducteur mince
 * vers `VibrationEffect` (stub non testable en JVM) : la décision du motif à jouer, y compris
 * la reprise après une vibration d'erreur, vit dans [VibrationPatternPolicy], testable.
 */
class AndroidVibrationController(
    private val context: Context,
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

    @Volatile
    private var repeating = false

    override fun startRepeating() {
        repeating = true
        play(VibrationPatternPolicy.repeating())
    }

    override fun vibrateError() {
        play(VibrationPatternPolicy.error(wasRepeating = repeating))
    }

    override fun stop() {
        repeating = false
        vibrator.cancel()
    }

    private fun play(pattern: VibrationPattern) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, pattern.repeatFromIndex))
    }
}
