package com.niumi.system.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/** Demande le focus audio avec les mêmes attributs que le lecteur (SPEC_ANDROID §10.2). */
class AndroidAudioFocusController(
    private val context: Context,
) : AudioFocusController {
    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var currentRequest: AudioFocusRequest? = null

    override fun request(configuration: AlarmAudioConfiguration): Boolean {
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(configuration.usage)
                .setContentType(configuration.contentType)
                .build()
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .build()
        currentRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun release() {
        currentRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        currentRequest = null
    }
}
