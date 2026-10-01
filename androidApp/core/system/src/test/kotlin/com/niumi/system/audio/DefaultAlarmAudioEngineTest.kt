package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import com.niumi.system.common.Clock
import com.niumi.system.common.OperationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Test

/**
 * SPEC_ANDROID §10.2 : la configuration audio demandée porte `USAGE_ALARM` +
 * `CONTENT_TYPE_SONIFICATION`, le focus est demandé avec la même configuration, `start()` est
 * idempotent, `stop()` libère lecteur + focus + vibration, et une exception du lecteur ne se
 * propage jamais. Lot 7 : la sonnerie demandée est celle qui joue, et la montée progressive est
 * pilotée ici, au pas de 250 ms, sur l'horloge injectée.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAlarmAudioEngineTest {
    private class FakePlayer : AlarmPlayer {
        var released = false
        val volumes = mutableListOf<Float>()

        override fun setVolume(amplitude: Float) {
            volumes += amplitude
        }

        override fun release() {
            released = true
        }
    }

    private val scope = TestScope()
    private val clock =
        object : Clock {
            override fun nowEpochMillis(): Long = T0 + scope.testScheduler.currentTime
        }

    private val fakePlayer = FakePlayer()
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
        DefaultAlarmAudioEngine(fakePlayerFactory, fakeFocusController, fakeVibrationController, clock, scope)

    private fun constant(vibrationEnabled: Boolean = false) = AlarmSound(KEY, vibrationEnabled, volumeRamp = null)

    private fun ramped(startedAt: Long = T0) = AlarmSound(KEY, false, VolumeRamp(MINUTE, startedAt))

    private fun advance(ms: Long) {
        scope.advanceTimeBy(ms)
        scope.runCurrent()
    }

    @Test
    fun startRequestsAlarmUsageAndSonificationContentType() {
        engine.start(constant())

        assertThat(lastConfiguration?.usage).isEqualTo(android.media.AudioAttributes.USAGE_ALARM)
        assertThat(lastConfiguration?.contentType)
            .isEqualTo(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
        assertThat(lastConfiguration?.looping).isTrue()
        assertThat(fakeFocusController.requestedConfiguration).isEqualTo(lastConfiguration)
        assertThat(lastRingtoneKey).isEqualTo(KEY)
    }

    @Test
    fun startEnablesVibrationOnlyWhenRequested() {
        engine.start(constant(vibrationEnabled = true))

        assertThat(fakeVibrationController.started).isTrue()
    }

    @Test
    fun startDoesNotEnableVibrationWhenNotRequested() {
        engine.start(constant(vibrationEnabled = false))

        assertThat(fakeVibrationController.started).isFalse()
    }

    @Test
    fun startTwiceIsIdempotentAndLaunchesASingleRamp() {
        engine.start(ramped())
        val second = engine.start(ramped())
        advance(VolumeRampPolicy.TICK_MS)

        assertThat(second).isEqualTo(OperationResult.AlreadySatisfied)
        assertThat(playerFactoryCallCount).isEqualTo(1)
        assertThat(fakePlayer.volumes).hasSize(1)
    }

    @Test
    fun startReportsIsPlayingTrue() {
        engine.start(constant())

        assertThat(engine.isPlaying).isTrue()
    }

    @Test
    fun constantVolumeStartsAtFullAmplitudeAndNeverAdjusts() {
        engine.start(constant())
        advance(2 * MINUTE)

        assertThat(lastConfiguration?.initialAmplitude).isEqualTo(1f)
        assertThat(fakePlayer.volumes).isEmpty()
    }

    @Test
    fun rampStartsAtMinusFortyDecibelsAndFollowsThePolicyEveryTick() {
        engine.start(ramped())

        assertThat(lastConfiguration?.initialAmplitude).isEqualTo(VolumeRampPolicy.START_AMPLITUDE)

        advance(VolumeRampPolicy.TICK_MS)
        assertThat(fakePlayer.volumes)
            .containsExactly(VolumeRampPolicy.amplitudeAt(VolumeRampPolicy.TICK_MS, MINUTE))

        advance(MINUTE)
        val ticks =
            (1..MINUTE / VolumeRampPolicy.TICK_MS).map {
                VolumeRampPolicy.amplitudeAt(it * VolumeRampPolicy.TICK_MS, MINUTE)
            }
        assertThat(fakePlayer.volumes).containsExactlyElementsIn(ticks).inOrder()
        assertThat(fakePlayer.volumes.last()).isEqualTo(1f)
    }

    @Test
    fun rampStopsAdjustingOnceComplete() {
        engine.start(ramped())
        advance(MINUTE)
        val adjustments = fakePlayer.volumes.size

        advance(MINUTE)

        assertThat(fakePlayer.volumes).hasSize(adjustments)
    }

    @Test
    fun aRampAlreadyUnderwayResumesAtTheElapsedAmplitude() {
        engine.start(ramped(startedAt = T0 - 45_000))

        assertThat(lastConfiguration?.initialAmplitude).isEqualTo(VolumeRampPolicy.amplitudeAt(45_000, MINUTE))

        advance(VolumeRampPolicy.TICK_MS)
        assertThat(fakePlayer.volumes)
            .containsExactly(VolumeRampPolicy.amplitudeAt(45_000 + VolumeRampPolicy.TICK_MS, MINUTE))

        advance(MINUTE)
        assertThat(fakePlayer.volumes).hasSize(15_000 / VolumeRampPolicy.TICK_MS.toInt())
        assertThat(fakePlayer.volumes.last()).isEqualTo(1f)
    }

    @Test
    fun aRampAlreadyCompleteStartsAtFullVolumeWithoutAdjusting() {
        engine.start(ramped(startedAt = T0 - 2 * MINUTE))
        advance(MINUTE)

        assertThat(lastConfiguration?.initialAmplitude).isEqualTo(1f)
        assertThat(fakePlayer.volumes).isEmpty()
    }

    @Test
    fun stopReleasesPlayerFocusAndVibration() {
        engine.start(constant(vibrationEnabled = true))
        val result = engine.stop()

        assertThat(fakePlayer.released).isTrue()
        assertThat(fakeFocusController.released).isTrue()
        assertThat(fakeVibrationController.stopped).isTrue()
        assertThat(engine.isPlaying).isFalse()
        assertThat(result).isEqualTo(OperationResult.Success)
    }

    @Test
    fun stopCancelsTheRamp() {
        engine.start(ramped())
        advance(VolumeRampPolicy.TICK_MS)
        engine.stop()

        advance(MINUTE)

        assertThat(fakePlayer.volumes).hasSize(1)
    }

    @Test
    fun stopWithoutStartIsAlreadySatisfied() {
        val result = engine.stop()

        assertThat(result).isEqualTo(OperationResult.AlreadySatisfied)
    }

    @Test
    fun playerFactoryExceptionIsCaughtAsFailureWithoutAnOrphanRamp() {
        playerFactoryThrows = true

        val result = engine.start(ramped())
        advance(MINUTE)

        assertThat(result).isInstanceOf(OperationResult.Failure::class.java)
        assertThat((result as OperationResult.Failure).code).isEqualTo("ANDROID_AUDIO_START_FAILED")
        assertThat(engine.isPlaying).isFalse()
        assertThat(fakePlayer.volumes).isEmpty()
    }

    private companion object {
        const val KEY = "niumi_oiseaux"
        const val T0 = 1_800_000_000_000L
        const val MINUTE = 60_000L
    }
}
