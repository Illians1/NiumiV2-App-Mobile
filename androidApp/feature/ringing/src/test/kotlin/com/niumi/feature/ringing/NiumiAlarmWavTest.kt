package com.niumi.feature.ringing

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vérifie l'en-tête WAV du fichier généré par `tools/generate_alarm_wav.py` (SPEC_ANDROID §10.2) :
 * mono, 16 bits, 44 100 Hz, durée 6 s ± 50 ms. Lecture directe du fichier statique plutôt que via
 * le système de ressources Android : aucune dépendance à un appareil, comme `ModuleListTest`
 * dans `:app`.
 */
class NiumiAlarmWavTest {
    @Test
    fun wavFileHasTheExpectedPcmFormatAndDuration() {
        val rootDir =
            File(
                requireNotNull(System.getProperty("niumi.rootDir")) {
                    "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
                },
            )
        val wavFile =
            File(rootDir, "androidApp/feature/ringing/src/main/res/raw/niumi_alarm.wav")
        assertThat(wavFile.exists()).isTrue()

        val bytes = wavFile.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        assertThat(readAscii(buffer, 4)).isEqualTo("RIFF")
        buffer.int // taille totale, non vérifiée
        assertThat(readAscii(buffer, 4)).isEqualTo("WAVE")
        assertThat(readAscii(buffer, 4)).isEqualTo("fmt ")
        val fmtChunkSize = buffer.int
        val audioFormat = buffer.short.toInt()
        val numChannels = buffer.short.toInt()
        val sampleRate = buffer.int
        buffer.int // byte rate, non vérifié
        buffer.short // block align, non vérifié
        val bitsPerSample = buffer.short.toInt()
        buffer.position(buffer.position() + (fmtChunkSize - FMT_CHUNK_MIN_SIZE))

        assertThat(audioFormat).isEqualTo(PCM_FORMAT)
        assertThat(numChannels).isEqualTo(1)
        assertThat(sampleRate).isEqualTo(44_100)
        assertThat(bitsPerSample).isEqualTo(16)

        assertThat(readAscii(buffer, 4)).isEqualTo("data")
        val dataChunkSize = buffer.int
        val bytesPerSecond = sampleRate * numChannels * (bitsPerSample / BITS_PER_BYTE)
        val durationMs = dataChunkSize * MS_PER_S / bytesPerSecond

        assertThat(durationMs).isIn(6_000L - DURATION_TOLERANCE_MS..6_000L + DURATION_TOLERANCE_MS)
    }

    private fun readAscii(
        buffer: ByteBuffer,
        length: Int,
    ): String {
        val chars = ByteArray(length)
        buffer.get(chars)
        return String(chars, Charsets.US_ASCII)
    }

    private companion object {
        const val PCM_FORMAT = 1
        const val FMT_CHUNK_MIN_SIZE = 16
        const val BITS_PER_BYTE = 8
        const val MS_PER_S = 1000L
        const val DURATION_TOLERANCE_MS = 50L
    }
}
