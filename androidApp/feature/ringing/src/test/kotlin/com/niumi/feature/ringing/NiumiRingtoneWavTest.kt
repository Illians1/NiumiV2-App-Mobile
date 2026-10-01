package com.niumi.feature.ringing

import com.google.common.truth.Truth.assertThat
import com.niumi.system.audio.NiumiRingtones
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vérifie l'en-tête WAV des quatre sonneries du catalogue (SPEC_ANDROID §10.2, Lot 7) : PCM
 * 16 bits, mono, 44 100 Hz, durée entre 5 et 20 s — le format normalisé à l'étape 26. Lecture
 * directe des fichiers statiques plutôt que via le système de ressources Android : aucune
 * dépendance à un appareil, comme `ModuleListTest` dans `:app`.
 *
 * Les blocs RIFF sont parcourus dans n'importe quel ordre et les inconnus ignorés : un fichier tel
 * que déposé portait un bloc `JUNK` avant `fmt `, un autre un bloc `C2PA` après `data`.
 */
class NiumiRingtoneWavTest {
    private val rawDir =
        File(
            File(
                requireNotNull(System.getProperty("niumi.rootDir")) {
                    "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
                },
            ),
            "androidApp/feature/ringing/src/main/res/raw",
        )

    @Test
    fun everyCatalogRingtoneIsAMono16BitPcmFileAt44100Hz() {
        NiumiRingtones.ALL.forEach { ringtone ->
            val file = File(rawDir, "${ringtone.key}.wav")
            assertThat(file.exists()).isTrue()

            val header = WavHeader.read(file.readBytes())

            assertThat(header.audioFormat).isEqualTo(PCM_FORMAT)
            assertThat(header.channels).isEqualTo(1)
            assertThat(header.sampleRate).isEqualTo(SAMPLE_RATE)
            assertThat(header.bitsPerSample).isEqualTo(BITS_PER_SAMPLE)
            assertThat(header.durationMs).isIn(MIN_DURATION_MS..MAX_DURATION_MS)
        }
    }

    @Test
    fun theLegacyRingtoneIsNoLongerPackaged() {
        assertThat(File(rawDir, "${NiumiRingtones.LEGACY_KEY}.wav").exists()).isFalse()
    }

    @Test
    fun rawDirectoryHoldsExactlyTheCatalogRingtones() {
        // Fichiers cachés (`.DS_Store` de macOS) exclus : aapt2 ne les empaquette pas.
        val packaged =
            rawDir
                .listFiles()
                .orEmpty()
                .map { it.name }
                .filterNot { it.startsWith(".") }
                .toSet()

        assertThat(packaged).containsExactlyElementsIn(NiumiRingtones.ALL.map { "${it.key}.wav" })
    }

    private data class WavHeader(
        val audioFormat: Int,
        val channels: Int,
        val sampleRate: Int,
        val bitsPerSample: Int,
        val dataSize: Long,
    ) {
        val durationMs: Long
            get() = dataSize * MS_PER_S / (sampleRate.toLong() * channels * (bitsPerSample / BITS_PER_BYTE))

        companion object {
            fun read(bytes: ByteArray): WavHeader {
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                check(buffer.ascii() == "RIFF")
                buffer.int // taille totale, non vérifiée
                check(buffer.ascii() == "WAVE")
                var format: IntArray? = null
                var dataSize: Long? = null
                while (buffer.remaining() >= CHUNK_HEADER_SIZE) {
                    val id = buffer.ascii()
                    val size = buffer.int.toLong() and UINT_MASK
                    val start = buffer.position()
                    when (id) {
                        "fmt " -> {
                            val audioFormat = buffer.short.toInt()
                            val channels = buffer.short.toInt()
                            val sampleRate = buffer.int
                            buffer.int // byte rate
                            buffer.short // block align
                            format = intArrayOf(audioFormat, channels, sampleRate, buffer.short.toInt())
                        }

                        "data" -> {
                            dataSize = size
                        }
                    }
                    // Les blocs RIFF sont alignés sur deux octets.
                    val next = start + size + (size and 1L)
                    if (next > buffer.limit()) break
                    buffer.position(next.toInt())
                }
                val fmt = checkNotNull(format) { "bloc fmt absent" }
                return WavHeader(fmt[0], fmt[1], fmt[2], fmt[3], checkNotNull(dataSize) { "bloc data absent" })
            }

            private fun ByteBuffer.ascii(): String {
                val chars = ByteArray(CHUNK_ID_SIZE)
                get(chars)
                return String(chars, Charsets.US_ASCII)
            }
        }
    }

    private companion object {
        const val PCM_FORMAT = 1
        const val SAMPLE_RATE = 44_100
        const val BITS_PER_SAMPLE = 16
        const val BITS_PER_BYTE = 8
        const val MS_PER_S = 1000L
        const val MIN_DURATION_MS = 5_000L
        const val MAX_DURATION_MS = 20_000L
        const val CHUNK_ID_SIZE = 4
        const val CHUNK_HEADER_SIZE = 8
        const val UINT_MASK = 0xFFFF_FFFFL
    }
}
