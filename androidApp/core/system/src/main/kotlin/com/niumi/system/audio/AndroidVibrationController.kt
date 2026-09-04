package com.niumi.system.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibration répétée pendant la sonnerie (SPEC_ANDROID §10.2), `minSdk 29`. */
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

    override fun startRepeating() {
        val pattern = longArrayOf(0, PULSE_DURATION_MS, PAUSE_DURATION_MS)
        val effect = VibrationEffect.createWaveform(pattern, REPEAT_FROM_INDEX)
        vibrator.vibrate(effect)
    }

    override fun stop() {
        vibrator.cancel()
    }

    private companion object {
        const val PULSE_DURATION_MS = 500L
        const val PAUSE_DURATION_MS = 500L
        const val REPEAT_FROM_INDEX = 0
    }
}
