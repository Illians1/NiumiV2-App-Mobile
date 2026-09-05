package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import com.niumi.system.common.OperationResult
import org.junit.Test

/**
 * SPEC_ANDROID §10.2 : la configuration audio demandée porte `USAGE_ALARM` +
 * `CONTENT_TYPE_SONIFICATION`, le focus est demandé avec la même configuration, `start()` est
 * idempotent, `stop()` libère lecteur + focus + vibration, et une exception du lecteur ne se
 * propage jamais.
 */
class DefaultAlarmAudioEngineTest {
    private val fakePlayer =
        object : AlarmPlayer {
            var released = false

            override fun release() {
                released = true
            }
        }

    private var playerFactoryCallCount = 0
    private var lastRingtoneKey: String? = null
    private var lastConfiguration: AlarmAudioConfiguration? = null
    private var playerFactoryThrows = false

    private val fakePlayerFactory =
        AlarmPlayerFactory { ringtoneKey, configuration ->
            playerFactoryCallCount++
            lastRingtoneKey = ringtoneKey
            lastConfiguration = configuration
            if (playerFactoryThrows) error("boom")
            fakePlayer
        }

    private val fakeFocusController =
        object : AudioFocusController {
            var requestedConfiguration: AlarmAudioConfiguration? = null
            var released = false

            override fun request(configuration: AlarmAudioConfiguration): Boolean {
                requestedConfiguration = configuration
                return true
            }

            override fun release() {
                released = true
            }
        }

    private val fakeVibrationController =
        object : VibrationController {
            var started = false
            var stopped = false

            override fun startRepeating() {
                started = true
            }

            override fun vibrateError() {
                // Non exercé par DefaultAlarmAudioEngine : AlarmActivity appelle
                // VibrationController.vibrateError() directement (voir VibrationPatternPolicyTest).
            }

            override fun stop() {
                stopped = true
            }
        }

    private val engine =
        DefaultAlarmAudioEngine(fakePlayerFactory, fakeFocusController, fakeVibrationController)

    @Test
    fun startRequestsAlarmUsageAndSonificationContentType() {
        engine.start(ringtoneKey = "niumi_alarm", vibrationEnabled = false)

        assertThat(lastConfiguration?.usage).isEqualTo(android.media.AudioAttributes.USAGE_ALARM)
        assertThat(lastConfiguration?.contentType)
            .isEqualTo(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
        assertThat(lastConfiguration?.looping).isTrue()
        assertThat(fakeFocusController.requestedConfiguration).isEqualTo(lastConfiguration)
        assertThat(lastRingtoneKey).isEqualTo("niumi_alarm")
    }

    @Test
    fun startEnablesVibrationOnlyWhenRequested() {
        engine.start(ringtoneKey = "niumi_alarm", vibrationEnabled = true)

        assertThat(fakeVibrationController.started).isTrue()
    }

    @Test
    fun startDoesNotEnableVibrationWhenNotRequested() {
        engine.start(ringtoneKey = "niumi_alarm", vibrationEnabled = false)

        assertThat(fakeVibrationController.started).isFalse()
    }

    @Test
    fun startTwiceIsIdempotent() {
        engine.start(ringtoneKey = "niumi_alarm", vibrationEnabled = false)
        engine.start(ringtoneKey = "niumi_alarm", vibrationEnabled = false)

        assertThat(playerFactoryCallCount).isEqualTo(1)
    }

    @Test
    fun startReportsIsPlayingTrue() {
        engine.start(ringtoneKey = "niumi_alarm", vibrationEnabled = false)

        assertThat(engine.isPlaying).isTrue()
    }

    @Test
    fun stopReleasesPlayerFocusAndVibration() {
        engine.start(ringtoneKey = "niumi_alarm", vibrationEnabled = true)
        val result = engine.stop()

        assertThat(fakePlayer.released).isTrue()
        assertThat(fakeFocusController.released).isTrue()
        assertThat(fakeVibrationController.stopped).isTrue()
        assertThat(engine.isPlaying).isFalse()
        assertThat(result).isEqualTo(OperationResult.Success)
    }

    @Test
    fun stopWithoutStartIsAlreadySatisfied() {
        val result = engine.stop()

        assertThat(result).isEqualTo(OperationResult.AlreadySatisfied)
    }

    @Test
    fun playerFactoryExceptionIsCaughtAsFailure() {
        playerFactoryThrows = true

        val result = engine.start(ringtoneKey = "niumi_alarm", vibrationEnabled = false)

        assertThat(result).isInstanceOf(OperationResult.Failure::class.java)
        assertThat((result as OperationResult.Failure).code).isEqualTo("ANDROID_AUDIO_START_FAILED")
        assertThat(engine.isPlaying).isFalse()
    }
}
