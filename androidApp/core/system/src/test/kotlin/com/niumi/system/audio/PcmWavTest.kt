package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lecteur d'en-tête WAV du lecteur de réveil `AudioTrack` (SPEC_ANDROID §10.2, boucle sans blanc,
 * décision du 2026-10-15). Il n'accepte que le format que `NiumiRingtoneWavTest` garantit pour le
 * catalogue — PCM 16 bits, mono, 44 100 Hz — et refuse tout le reste par une exception, que
 * `DefaultAlarmAudioEngine` transforme en `ANDROID_AUDIO_START_FAILED` : jamais un crash.
 */
class PcmWavTest {
    private val samples = ByteArray(8) { it.toByte() } // 4 images mono 16 bits

    @Test
    fun aCanonicalFileYieldsItsSamplesAndFrameCount() {
        val wav = PcmWav.read(wav(samples))

        assertThat(wav.sampleRate).isEqualTo(44_100)
        assertThat(wav.frameCount).isEqualTo(4)
        assertThat(wav.data).isEqualTo(samples)
    }

    /** Un fichier tel que déposé portait un bloc `JUNK` avant `fmt `, un autre `LIST` avant `data`. */
    @Test
    fun unknownChunksBeforeTheDataAreSkipped() {
        val wav = PcmWav.read(wav(samples, extraChunkBeforeData = "LIST" to ByteArray(6)))

        assertThat(wav.data).isEqualTo(samples)
    }

    @Test
    fun aStereoFileIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { PcmWav.read(wav(samples, Fmt(channels = 2))) }
    }

    @Test
    fun anEightBitFileIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { PcmWav.read(wav(samples, Fmt(bitsPerSample = 8))) }
    }

    @Test
    fun anotherSampleRateIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { PcmWav.read(wav(samples, Fmt(sampleRate = 48_000))) }
    }

    @Test
    fun aFloatingPointFileIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { PcmWav.read(wav(samples, Fmt(audioFormat = 3))) }
    }

    @Test
    fun aTruncatedHeaderIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { PcmWav.read(wav(samples).copyOf(20)) }
    }

    @Test
    fun aDataChunkLongerThanTheFileIsRefused() {
        val truncated = wav(samples).let { it.copyOf(it.size - 4) }

        assertThrows(IllegalArgumentException::class.java) { PcmWav.read(truncated) }
    }

    @Test
    fun aFileWithoutRiffMarkerIsRefused() {
        val notWav = wav(samples).also { it[0] = 'X'.code.toByte() }

        assertThrows(IllegalArgumentException::class.java) { PcmWav.read(notWav) }
    }

    /** Champs du bloc `fmt `, au format du catalogue par défaut. */
    private data class Fmt(
        val audioFormat: Int = 1,
        val channels: Int = 1,
        val sampleRate: Int = 44_100,
        val bitsPerSample: Int = 16,
    )

    private fun wav(
        data: ByteArray,
        format: Fmt = Fmt(),
        extraChunkBeforeData: Pair<String, ByteArray>? = null,
    ): ByteArray {
        val body = ByteArrayOutputStream()
        body.write("WAVE".toByteArray(Charsets.US_ASCII))
        body.write(chunk("fmt ", fmt(format)))
        extraChunkBeforeData?.let { (id, payload) -> body.write(chunk(id, payload)) }
        body.write(chunk("data", data))
        val bodyBytes = body.toByteArray()
        return ByteArrayOutputStream()
            .apply {
                write("RIFF".toByteArray(Charsets.US_ASCII))
                write(le32(bodyBytes.size))
                write(bodyBytes)
            }.toByteArray()
    }

    private fun fmt(format: Fmt): ByteArray =
        ByteBuffer
            .allocate(16)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(format.audioFormat.toShort())
            .putShort(format.channels.toShort())
            .putInt(format.sampleRate)
            .putInt(format.sampleRate * format.channels * format.bitsPerSample / 8)
            .putShort((format.channels * format.bitsPerSample / 8).toShort())
            .putShort(format.bitsPerSample.toShort())
            .array()

    private fun chunk(
        id: String,
        payload: ByteArray,
    ): ByteArray =
        ByteArrayOutputStream()
            .apply {
                write(id.toByteArray(Charsets.US_ASCII))
                write(le32(payload.size))
                write(payload)
                if (payload.size % 2 == 1) write(0)
            }.toByteArray()

    private fun le32(value: Int): ByteArray =
        ByteBuffer
            .allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(value)
            .array()
}
