package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Un réveil muet n'est jamais acceptable (SPEC_ANDROID §10.2) : si le lecteur sans blanc
 * (`AudioTrack` statique) ne peut pas être créé, le lecteur `MediaPlayer` prend le relais — avec
 * son blanc de boucle, mais sonore.
 */
class FallbackAlarmPlayerFactoryTest {
    private class Player(
        override val gaplessLoop: Boolean,
    ) : AlarmPlayer {
        override fun setVolume(amplitude: Float) = Unit

        override fun release() = Unit
    }

    private val configuration =
        AlarmAudioConfiguration(
            usage = 4,
            contentType = 4,
            looping = true,
            vibrationEnabled = true,
            initialAmplitude = 0.01f,
        )

    private val gapless = Player(gaplessLoop = true)
    private val withGap = Player(gaplessLoop = false)
    private val fallbackCalls = mutableListOf<Pair<String, AlarmAudioConfiguration>>()

    private val fallback =
        AlarmPlayerFactory { key, config ->
            fallbackCalls += key to config
            withGap
        }

    /** Ce que fait `AudioTrackAlarmPlayerFactory` quand l'appareil refuse son tampon statique. */
    private val refusedPrimary = AlarmPlayerFactory { _, _ -> error("tampon statique refusé") }

    @Test
    fun thePrimaryPlayerIsUsedWhenItCanBeCreated() {
        val factory = FallbackAlarmPlayerFactory(primary = { _, _ -> gapless }, fallback = fallback)

        val player = factory.create("niumi_oiseaux", configuration)

        assertThat(player).isSameInstanceAs(gapless)
        assertThat(player.gaplessLoop).isTrue()
        assertThat(fallbackCalls).isEmpty()
    }

    @Test
    fun aPrimaryThatThrowsHandsOverToTheFallbackWithTheSameRequest() {
        val factory = FallbackAlarmPlayerFactory(primary = refusedPrimary, fallback = fallback)

        val player = factory.create("niumi_oiseaux", configuration)

        assertThat(player).isSameInstanceAs(withGap)
        assertThat(player.gaplessLoop).isFalse()
        assertThat(fallbackCalls).containsExactly("niumi_oiseaux" to configuration)
    }

    /** Les deux échouent : l'exception remonte, le moteur la transforme en `ANDROID_AUDIO_START_FAILED`. */
    @Test
    fun whenBothFailTheFallbackFailureReachesTheEngine() {
        val factory =
            FallbackAlarmPlayerFactory(
                primary = refusedPrimary,
                fallback = { _, _ -> throw IllegalStateException("MediaPlayer indisponible") },
            )

        val thrown = assertThrows(IllegalStateException::class.java) { factory.create("niumi_oiseaux", configuration) }

        assertThat(thrown).hasMessageThat().isEqualTo("MediaPlayer indisponible")
    }
}
