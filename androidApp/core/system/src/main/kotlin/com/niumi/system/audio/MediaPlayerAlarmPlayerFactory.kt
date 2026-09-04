package com.niumi.system.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Traduit [AlarmAudioConfiguration] vers un `MediaPlayer` réel, avec une ressource locale
 * empaquetée dans l'APK (SPEC_ANDROID §10.2). Ne dépend d'aucune URI réseau ni fournisseur de
 * documents. Non testable en JVM (`MediaPlayer` et `AudioAttributes.Builder` sont des stubs
 * hors d'un appareil) : couvert par les tests instrumentés de l'étape.
 */
class MediaPlayerAlarmPlayerFactory(
    private val context: Context,
    private val ringtoneResolver: RingtoneResourceResolver,
) : AlarmPlayerFactory {
    override fun create(
        ringtoneKey: String,
        configuration: AlarmAudioConfiguration,
    ): AlarmPlayer {
        val resourceId =
            requireNotNull(ringtoneResolver.resourceId(ringtoneKey)) {
                "Sonnerie inconnue : $ringtoneKey"
            }
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(configuration.usage)
                .setContentType(configuration.contentType)
                .build()
        val mediaPlayer =
            MediaPlayer().apply {
                setAudioAttributes(attributes)
                val descriptor = context.resources.openRawResourceFd(resourceId)
                descriptor.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                isLooping = configuration.looping
                prepare()
                start()
            }
        return object : AlarmPlayer {
            override fun release() {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
    }
}
