package com.niumi.system.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Échantillons d'un fichier de sonnerie, prêts pour un `AudioTrack` statique (SPEC_ANDROID §10.2,
 * boucle sans blanc, décision du 2026-10-15). Pur Kotlin, testable en JVM.
 *
 * N'accepte que le format du catalogue — PCM 16 bits, mono, 44 100 Hz, que `NiumiRingtoneWavTest`
 * garantit fichier par fichier — et refuse tout le reste par une [IllegalArgumentException] :
 * `DefaultAlarmAudioEngine` l'attrape (`ANDROID_AUDIO_START_FAILED`), et `FallbackAlarmPlayerFactory`
 * passe alors au lecteur `MediaPlayer`, qui sait lire d'autres formats. Les blocs RIFF sont
 * parcourus dans n'importe quel ordre et les inconnus ignorés, comme dans `NiumiRingtoneWavTest`.
 */
class PcmWav private constructor(
    val sampleRate: Int,
    val data: ByteArray,
) {
    /** Nombre d'images (un échantillon de 16 bits par image en mono). */
    val frameCount: Int get() = data.size / BYTES_PER_FRAME

    companion object {
        const val SAMPLE_RATE: Int = 44_100
        private const val PCM_FORMAT = 1
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val BYTES_PER_FRAME = CHANNELS * BITS_PER_SAMPLE / 8
        private const val RIFF_HEADER_SIZE = 12
        private const val CHUNK_HEADER_SIZE = 8
        private const val FMT_MIN_SIZE = 16
        private const val ID_SIZE = 4

        fun read(bytes: ByteArray): PcmWav {
            require(bytes.size >= RIFF_HEADER_SIZE) { "En-tête RIFF tronqué" }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(buffer.ascii() == "RIFF") { "Pas un fichier RIFF" }
            buffer.int // taille RIFF, non fiable : on se fie aux blocs
            require(buffer.ascii() == "WAVE") { "Pas un fichier WAVE" }

            var formatSeen = false
            while (buffer.remaining() >= CHUNK_HEADER_SIZE) {
                val id = buffer.ascii()
                val size = buffer.int
                require(size >= 0 && size <= buffer.remaining()) { "Bloc $id plus long que le fichier" }
                val payloadStart = buffer.position()
                when (id) {
                    "fmt " -> {
                        checkFormat(buffer, size)
                        formatSeen = true
                    }

                    "data" -> {
                        require(formatSeen) { "Bloc data avant le bloc fmt" }
                        require(size % BYTES_PER_FRAME == 0) { "Bloc data incomplet" }
                        return PcmWav(SAMPLE_RATE, bytes.copyOfRange(payloadStart, payloadStart + size))
                    }
                }
                // Un bloc de taille impaire est suivi d'un octet de bourrage (RIFF).
                val next = payloadStart + size + (size and 1)
                buffer.position(minOf(next, bytes.size))
            }
            throw IllegalArgumentException("Bloc data absent")
        }

        private fun checkFormat(
            buffer: ByteBuffer,
            size: Int,
        ) {
            require(size >= FMT_MIN_SIZE) { "Bloc fmt tronqué" }
            val audioFormat = buffer.short.toInt()
            val channels = buffer.short.toInt()
            val sampleRate = buffer.int
            buffer.int // débit en octets
            buffer.short // alignement de bloc
            val bitsPerSample = buffer.short.toInt()
            require(audioFormat == PCM_FORMAT) { "Format $audioFormat non PCM" }
            require(channels == CHANNELS) { "$channels canaux, mono attendu" }
            require(sampleRate == SAMPLE_RATE) { "$sampleRate Hz, $SAMPLE_RATE attendus" }
            require(bitsPerSample == BITS_PER_SAMPLE) { "$bitsPerSample bits, 16 attendus" }
        }

        private fun ByteBuffer.ascii(): String {
            val id = ByteArray(ID_SIZE)
            get(id)
            return String(id, Charsets.US_ASCII)
        }
    }
}
