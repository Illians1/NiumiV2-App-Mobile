package com.niumi.system.audio

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
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
 *
 * **Usage alarme (2026-11-02).** Pendant une sonnerie, la vibration porte l'usage alarme, comme le
 * son : sans attributs (`usage: UNKNOWN`), elle était ignorée quand une autre app d'alarme vibrait
 * au même moment (`ignored_for_higher_importance`, mesuré sur Xiaomi). La vibration d'erreur hors
 * sonnerie garde l'appel sans attributs.
 *
 * **Relance à l'extinction de l'écran (2026-11-02).** Android annule la vibration en cours de toute
 * app non système, quel que soit son usage, quand l'écran s'éteint autrement que par délai ou
 * inactivité, le bouton marche/arrêt par exemple (AOSP,
 * `VibrationSettings.shouldCancelVibrationOnScreenOff`) ; une vibration démarrée après l'extinction
 * n'est pas annulée. Tant que la sonnerie vibre, chaque `ACTION_SCREEN_OFF` rejoue donc le motif au
 * temps écoulé.
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

    private val screenOffReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                ongoing?.let { playAlarm(patternFor(it)) }
            }
        }

    private var screenOffReceiverRegistered = false

    override fun startRepeating(ramp: VolumeRamp?) {
        val started = Ongoing(ramp)
        ongoing = started
        registerScreenOffReceiver()
        playAlarm(patternFor(started))
    }

    override fun vibrateError() {
        val current = ongoing
        if (current == null) {
            vibrator.vibrate(VibrationPatternPolicy.error(resume = null).toEffect())
        } else {
            playAlarm(VibrationPatternPolicy.error(resume = patternFor(current)))
        }
    }

    override fun stop() {
        ongoing = null
        unregisterScreenOffReceiver()
        vibrator.cancel()
    }

    private fun patternFor(current: Ongoing): VibrationPattern {
        val ramp = current.ramp ?: return VibrationPatternPolicy.repeating()
        return VibrationPatternPolicy.repeating(
            elapsedMs = clock.nowEpochMillis() - ramp.startedAtEpochMillis,
            durationMs = ramp.durationMs,
        )
    }

    private fun playAlarm(pattern: VibrationPattern) {
        val effect = pattern.toEffect()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, ALARM_AUDIO_ATTRIBUTES)
        }
    }

    // Diffusion système protégée : reçue malgré `RECEIVER_NOT_EXPORTED` (voir `AlarmVolumeBroadcasts`).
    // La constante, inlinée, est ignorée avant l'API 33, où l'indicateur n'existe pas.
    @SuppressLint("InlinedApi")
    @Synchronized
    private fun registerScreenOffReceiver() {
        if (screenOffReceiverRegistered) return
        context.registerReceiver(
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            Context.RECEIVER_NOT_EXPORTED,
        )
        screenOffReceiverRegistered = true
    }

    @Synchronized
    private fun unregisterScreenOffReceiver() {
        if (!screenOffReceiverRegistered) return
        try {
            context.unregisterReceiver(screenOffReceiver)
        } catch (_: IllegalArgumentException) {
            // Déjà désenregistré : rien à faire.
        }
        screenOffReceiverRegistered = false
    }

    private fun VibrationPattern.toEffect(): VibrationEffect = VibrationEffect.createWaveform(timings, repeatFromIndex)

    private companion object {
        val ALARM_AUDIO_ATTRIBUTES: AudioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
    }
}
