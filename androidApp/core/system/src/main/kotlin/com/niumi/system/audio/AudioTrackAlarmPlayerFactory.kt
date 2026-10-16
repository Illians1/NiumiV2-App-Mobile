package com.niumi.system.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Lecteur de réveil à boucle sans blanc (SPEC_ANDROID §10.2, décision du 2026-10-15) : le son
 * entier est chargé dans un `AudioTrack` statique, et des points de boucle posés sur la totalité
 * le font rejouer en continu, à l'échantillon près. `MediaPlayer.isLooping`, lui, arrête puis
 * relance la piste à chaque tour : 70 à 430 ms de silence mesurés sur « Oiseaux », audibles.
 *
 * Mêmes attributs et même règle d'amplitude que `MediaPlayerAlarmPlayerFactory` : `USAGE_ALARM`,
 * amplitude initiale appliquée **avant** `play()`, pour qu'une montée progressive ne laisse jamais
 * passer un premier échantillon à plein volume. Non testable en JVM (`AudioTrack` est un stub hors
 * d'un appareil) : couvert par `AlarmChainInstrumentedTest`. Peut lancer — tampon statique refusé,
 * fichier hors format — : `FallbackAlarmPlayerFactory` passe alors au lecteur `MediaPlayer`.
 */
class AudioTrackAlarmPlayerFactory(
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
        val wav = PcmWav.read(context.resources.openRawResource(resourceId).use { it.readBytes() })
        val track =
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(configuration.usage)
                        .setContentType(configuration.contentType)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(wav.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(wav.data.size)
                .build()
        try {
            check(track.state == AudioTrack.STATE_NO_STATIC_DATA) { "AudioTrack statique non initialisé" }
            val written = track.write(wav.data, 0, wav.data.size)
            check(written == wav.data.size) { "Écriture partielle du tampon statique : $written" }
            if (configuration.looping) {
                check(track.setLoopPoints(0, wav.frameCount, LOOP_FOREVER) == AudioTrack.SUCCESS) {
                    "Points de boucle refusés"
                }
            }
            track.setVolume(configuration.initialAmplitude)
            track.play()
        } catch (error: IllegalStateException) {
            track.release()
            throw error
        }
        return object : AlarmPlayer {
            override val gaplessLoop: Boolean = configuration.looping

            override fun setVolume(amplitude: Float) {
                track.setVolume(amplitude)
            }

            override fun release() {
                track.stop()
                track.release()
            }
        }
    }

    private companion object {
        /** `setLoopPoints` : un nombre de boucles négatif signifie « à l'infini ». */
        const val LOOP_FOREVER = -1
    }
}
