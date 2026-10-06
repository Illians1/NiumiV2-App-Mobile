package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import com.niumi.system.common.OperationResult
import org.junit.Test

/**
 * SPEC_ANDROID §10.2, pré-écoute (Lot 7) : mêmes attributs `USAGE_ALARM` que le réveil, une seule
 * lecture sans boucle ni montée ni vibration, une seule pré-écoute à la fois.
 */
class RingtonePreviewPlayerTest {
    private class FakePlayer(
        val key: String,
        val onCompletion: () -> Unit,
    ) : AlarmPlayer {
        var released = false

        override fun setVolume(amplitude: Float) = Unit

        override fun release() {
            released = true
        }
    }

    private val created = mutableListOf<FakePlayer>()
    private var lastConfiguration: AlarmAudioConfiguration? = null

    private val factory =
        PreviewPlayerFactory { ringtoneKey, configuration, onCompletion ->
            requireNotNull(NiumiRingtones.byKey(ringtoneKey)) { "Sonnerie inconnue : $ringtoneKey" }
            lastConfiguration = configuration
            FakePlayer(ringtoneKey, onCompletion).also { created += it }
        }

    private val focus =
        object : AudioFocusController {
            var requests = 0
            var releases = 0

            override fun request(configuration: AlarmAudioConfiguration): Boolean {
                requests++
                return true
            }

            override fun release() {
                releases++
            }
        }

    private val preview = DefaultRingtonePreviewPlayer(factory, focus)

    @Test
    fun playRequestsFocusAndCreatesAOneShotAlarmPlayerAtFullVolume() {
        val result = preview.play("niumi_piano")

        assertThat(result).isEqualTo(OperationResult.Success)
        assertThat(focus.requests).isEqualTo(1)
        assertThat(created.single().key).isEqualTo("niumi_piano")
        assertThat(lastConfiguration?.usage).isEqualTo(android.media.AudioAttributes.USAGE_ALARM)
        assertThat(lastConfiguration?.looping).isFalse()
        assertThat(lastConfiguration?.vibrationEnabled).isFalse()
        assertThat(lastConfiguration?.initialAmplitude).isEqualTo(1f)
        assertThat(preview.isPlaying).isTrue()
        assertThat(preview.playingKey.value).isEqualTo("niumi_piano")
    }

    @Test
    fun playingAnotherRingtoneReleasesTheFirst() {
        preview.play("niumi_piano")
        preview.play("niumi_oiseaux")

        assertThat(created.map { it.released }).containsExactly(true, false).inOrder()
        assertThat(preview.isPlaying).isTrue()
        assertThat(preview.playingKey.value).isEqualTo("niumi_oiseaux")
    }

    @Test
    fun stopReleasesPlayerAndFocusAndIsIdempotent() {
        preview.play("niumi_bell")
        preview.stop()
        preview.stop()

        assertThat(created.single().released).isTrue()
        assertThat(focus.releases).isEqualTo(1)
        assertThat(preview.isPlaying).isFalse()
        assertThat(preview.playingKey.value).isNull()
    }

    @Test
    fun theEndOfThePlaybackReleasesEverythingAndClearsThePlayingKey() {
        preview.play("niumi_bell")
        created.single().onCompletion()

        assertThat(created.single().released).isTrue()
        assertThat(focus.releases).isEqualTo(1)
        assertThat(preview.isPlaying).isFalse()
        assertThat(preview.playingKey.value).isNull()
    }

    @Test
    fun aStaleCompletionDoesNotStopTheCurrentPreview() {
        preview.play("niumi_bell")
        preview.play("niumi_piano")
        created.first().onCompletion()

        assertThat(created.last().released).isFalse()
        assertThat(preview.isPlaying).isTrue()
        assertThat(preview.playingKey.value).isEqualTo("niumi_piano")
    }

    @Test
    fun anUnknownRingtoneFailsWithoutAPlayerAndGivesTheFocusBack() {
        val result = preview.play("niumi_alarm")

        assertThat(result).isInstanceOf(OperationResult.Failure::class.java)
        assertThat((result as OperationResult.Failure).code).isEqualTo("ANDROID_AUDIO_START_FAILED")
        assertThat(created).isEmpty()
        assertThat(focus.releases).isEqualTo(focus.requests)
        assertThat(preview.isPlaying).isFalse()
        assertThat(preview.playingKey.value).isNull()
    }
}
