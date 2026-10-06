package com.niumi.system.audio

import android.media.AudioAttributes
import com.niumi.system.common.OperationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [RingtonePreviewPlayer] : mêmes attributs `USAGE_ALARM` que le réveil — le volume entendu est
 * celui qui sonnera —, une seule lecture sans boucle, sans montée, sans vibration, avec un focus
 * transitoire rendu à la fin (SPEC_ANDROID §10.2).
 */
class DefaultRingtonePreviewPlayer(
    private val playerFactory: PreviewPlayerFactory,
    private val focusController: AudioFocusController,
) : RingtonePreviewPlayer {
    private val lock = Any()
    private var player: AlarmPlayer? = null
    private val mutablePlayingKey = MutableStateFlow<String?>(null)

    override val isPlaying: Boolean
        get() = synchronized(lock) { player != null }

    override val playingKey: StateFlow<String?> = mutablePlayingKey.asStateFlow()

    override fun play(ringtoneKey: String): OperationResult =
        synchronized(lock) {
            releaseLocked()
            val configuration =
                AlarmAudioConfiguration(
                    usage = AudioAttributes.USAGE_ALARM,
                    contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION,
                    looping = false,
                    vibrationEnabled = false,
                    initialAmplitude = 1f,
                )
            focusController.request(configuration)
            // Même contrat que DefaultAlarmAudioEngine : une fabrique MediaPlayer peut lever
            // n'importe quelle exception d'exécution, et une pré-écoute ne doit jamais faire
            // planter l'écran qui l'a demandée.
            @Suppress("TooGenericExceptionCaught")
            try {
                lateinit var created: AlarmPlayer
                created = playerFactory.create(ringtoneKey, configuration) { onFinished(created) }
                player = created
                mutablePlayingKey.value = ringtoneKey
                OperationResult.Success
            } catch (error: RuntimeException) {
                focusController.release()
                OperationResult.Failure("ANDROID_AUDIO_START_FAILED", error)
            }
        }

    override fun stop() {
        synchronized(lock) { releaseLocked() }
    }

    private fun onFinished(finished: AlarmPlayer) {
        synchronized(lock) {
            if (player === finished) releaseLocked()
        }
    }

    private fun releaseLocked() {
        val current = player ?: return
        player = null
        current.release()
        focusController.release()
        mutablePlayingKey.value = null
    }
}
