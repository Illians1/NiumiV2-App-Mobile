package com.niumi.system.audio

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/**
 * Volume du flux d'alarme (SPEC_ANDROID §7.1 `SessionRuntimeStatus`, champ `audioReady` ;
 * §10.2, ligne « Volume du téléphone » de la feuille Sonnerie, étape 40, suivie en direct depuis
 * le 2026-10-24 par [changes]).
 */
interface AlarmVolumeSource {
    fun alarmStreamVolume(): Int

    /** Pourcentage affiché par la feuille Sonnerie, `null` si le flux n'est pas lisible. */
    fun alarmStreamPercent(): Int?

    /** Le volume d'alarme a pu changer : à relire. Émis tant que le flux est collecté. */
    fun changes(): Flow<Unit>
}

/**
 * Fonction pure derrière [AndroidAlarmVolumeSource.alarmStreamPercent], testable en JVM sans
 * `AudioManager` : un maximum non positif (flux illisible) rend `null` plutôt qu'une division par
 * zéro ou un pourcentage inventé.
 */
internal fun alarmVolumePercent(
    volume: Int,
    max: Int,
): Int? = if (max <= 0) null else ((volume.toFloat() / max) * PERCENT_SCALE).roundToInt()

private const val PERCENT_SCALE = 100

class AndroidAlarmVolumeSource(
    private val context: Context,
) : AlarmVolumeSource {
    private val audioManager get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun alarmStreamVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

    override fun alarmStreamPercent(): Int? =
        alarmVolumePercent(
            volume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM),
            max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
        )

    override fun changes(): Flow<Unit> = context.alarmVolumeChanges()
}
