package com.niumi.system.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Traduit [AlarmAudioConfiguration] vers un `MediaPlayer` réel, avec une ressource locale
 * empaquetée dans l'APK (SPEC_ANDROID §10.2). Ne dépend d'aucune URI réseau ni fournisseur de
 * documents. Non testable en JVM (`MediaPlayer` et `AudioAttributes.Builder` sont des stubs
 * hors d'un appareil) : couvert par les tests instrumentés.
 *
 * Sert le réveil ([AlarmPlayerFactory]) et la pré-écoute ([PreviewPlayerFactory], Lot 7) : un seul
 * endroit construit un `MediaPlayer`, donc un seul jeu d'attributs `USAGE_ALARM`.
 */
class MediaPlayerAlarmPlayerFactory(
    private val context: Context,
    private val ringtoneResolver: RingtoneResourceResolver,
) : AlarmPlayerFactory,
    PreviewPlayerFactory {
    override fun create(
        ringtoneKey: String,
        configuration: AlarmAudioConfiguration,
    ): AlarmPlayer = build(ringtoneKey, configuration, onCompletion = null)

    override fun create(
        ringtoneKey: String,
        configuration: AlarmAudioConfiguration,
        onCompletion: () -> Unit,
    ): AlarmPlayer = build(ringtoneKey, configuration, onCompletion)

    private fun build(
        ringtoneKey: String,
        configuration: AlarmAudioConfiguration,
        onCompletion: (() -> Unit)?,
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
                if (onCompletion != null) setOnCompletionListener { onCompletion() }
                prepare()
                // Avant start() : une montée progressive ne doit jamais laisser passer un premier
                // échantillon à plein volume.
                setVolume(configuration.initialAmplitude, configuration.initialAmplitude)
                start()
            }
        return object : AlarmPlayer {
            override fun setVolume(amplitude: Float) {
                mediaPlayer.setVolume(amplitude, amplitude)
            }

            override fun release() {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
    }
}
