package com.niumi.feature.session.ringtone

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.feature.session.active.fakes.FakeSessionPersistenceGateway
import com.niumi.feature.session.active.fakes.presentSession
import com.niumi.feature.session.ringtone.fakes.FakeAlarmSoundPreferences
import com.niumi.feature.session.ringtone.fakes.FakeAlarmVolumeSource
import com.niumi.feature.session.ringtone.fakes.FakeRingtonePreviewPlayer
import com.niumi.feature.session.ringtone.fakes.FakeSessionCoordinator
import com.niumi.feature.session.ringtone.fakes.PREFERENCE_SETTINGS
import com.niumi.feature.session.ringtone.fakes.SESSION_EXTRAS_SETTINGS
import com.niumi.feature.session.ringtone.fakes.sessionExtras
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.VolumeRampDurations
import com.niumi.system.session.AlarmSoundUpdateResult
import com.niumi.system.session.LoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Écran 14 (SPEC_ANDROID §15, Lot 7). Deux modes selon la session lue par la passerelle : une
 * session `Present` en `ARMED` fait foi (ce qui sonnera), sinon la préférence seule.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RingtoneViewModelTest {
    private val preferences = FakeAlarmSoundPreferences(settings = PREFERENCE_SETTINGS)
    private val previewPlayer = FakeRingtonePreviewPlayer()
    private val volumeSource = FakeAlarmVolumeSource()
    private val coordinator = FakeSessionCoordinator()
    private val gateway = FakeSessionPersistenceGateway()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): RingtoneViewModel =
        RingtoneViewModel(preferences, previewPlayer, volumeSource, coordinator, gateway)

    private fun armedSnapshot(state: SessionStateDto = SessionStateDto.ARMED) =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 1,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule =
                WakeScheduleDto(
                    localDateIso = "2026-09-04",
                    localTimeIso = "07:00",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = 1L,
                ),
            state = state,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1L,
            armedAtEpochMillis = 1L,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    // Mode : préférence hors session.

    @Test
    fun withoutAnArmedSessionTheInitialStateIsThePreference() {
        gateway.result = LoadResult.Absent

        val viewModel = viewModel()

        assertThat(viewModel.state.settings).isEqualTo(PREFERENCE_SETTINGS)
        assertThat(viewModel.state.isSessionArmed).isFalse()
    }

    @Test
    fun aSessionThatIsNotArmedIsIgnoredEvenIfPresent() {
        gateway.result = presentSession(armedSnapshot(SessionStateDto.RINGING), emptyList())

        val viewModel = viewModel()

        assertThat(viewModel.state.settings).isEqualTo(PREFERENCE_SETTINGS)
        assertThat(viewModel.state.isSessionArmed).isFalse()
    }

    @Test
    fun selectingARingtoneWritesThePreference() {
        gateway.result = LoadResult.Absent
        val viewModel = viewModel()

        viewModel.onRingtoneSelected("niumi_oiseaux")

        assertThat(viewModel.state.settings.ringtoneKey).isEqualTo("niumi_oiseaux")
        assertThat(preferences.written?.ringtoneKey).isEqualTo("niumi_oiseaux")
        assertThat(coordinator.updateCalls).isEqualTo(0)
    }

    @Test
    fun enablingTheRampProposesTheDefaultDurationAndDisablingItClearsIt() {
        gateway.result = LoadResult.Absent
        val viewModel = viewModel()

        viewModel.onVolumeRampEnabledChanged(enabled = true)
        assertThat(viewModel.state.settings.volumeRampSeconds).isEqualTo(VolumeRampDurations.DEFAULT_SECONDS)

        viewModel.onVolumeRampEnabledChanged(enabled = false)
        assertThat(viewModel.state.settings.volumeRampSeconds).isNull()
    }

    @Test
    fun aChosenDurationIsWrittenAsIs() {
        gateway.result = LoadResult.Absent
        val viewModel = viewModel()

        viewModel.onVolumeRampSecondsChanged(300)

        assertThat(viewModel.state.settings.volumeRampSeconds).isEqualTo(300)
    }

    @Test
    fun aFailedWriteShowsAMessageAndKeepsTheChosenValueDisplayed() {
        gateway.result = LoadResult.Absent
        preferences.writeFails = true
        val viewModel = viewModel()

        viewModel.onRingtoneSelected("niumi_bell")

        assertThat(viewModel.state.settings.ringtoneKey).isEqualTo("niumi_bell")
        assertThat(viewModel.state.message).isEqualTo(RingtoneTexts.SAVE_FAILED)
    }

    // Mode : session ARMED, valeurs initiales de la session.

    @Test
    fun anArmedSessionSuppliesTheInitialValuesInsteadOfThePreference() {
        gateway.result = presentSession(armedSnapshot(), emptyList()).copy(extras = sessionExtras())

        val viewModel = viewModel()

        assertThat(viewModel.state.settings).isEqualTo(SESSION_EXTRAS_SETTINGS)
        assertThat(viewModel.state.isSessionArmed).isTrue()
    }

    @Test
    fun aChangeInArmedModeIsAppliedToTheSessionAfterThePreference() {
        gateway.result = presentSession(armedSnapshot(), emptyList()).copy(extras = sessionExtras())
        val viewModel = viewModel()

        viewModel.onRingtoneSelected("niumi_piano")

        assertThat(preferences.written?.ringtoneKey).isEqualTo("niumi_piano")
        assertThat(coordinator.updateCalls).isEqualTo(1)
        assertThat(coordinator.lastRingtoneKey).isEqualTo("niumi_piano")
        assertThat(viewModel.state.message).isNull()
    }

    @Test
    fun aSessionThatLeftArmedInTheMeantimeShowsAMessageAndLeavesTheBanner() {
        gateway.result = presentSession(armedSnapshot(), emptyList()).copy(extras = sessionExtras())
        val viewModel = viewModel()
        coordinator.result = AlarmSoundUpdateResult.NotArmed

        viewModel.onRingtoneSelected("niumi_piano")

        assertThat(viewModel.state.isSessionArmed).isFalse()
        assertThat(viewModel.state.message).isEqualTo(RingtoneTexts.NOT_ARMED_MESSAGE)
    }

    @Test
    fun aSessionThatDisappearedInTheMeantimeSilentlyLeavesTheBanner() {
        gateway.result = presentSession(armedSnapshot(), emptyList()).copy(extras = sessionExtras())
        val viewModel = viewModel()
        coordinator.result = AlarmSoundUpdateResult.NoActiveSession

        viewModel.onRingtoneSelected("niumi_piano")

        assertThat(viewModel.state.isSessionArmed).isFalse()
        assertThat(viewModel.state.message).isNull()
    }

    @Test
    fun anUnexpectedCoordinatorRefusalShowsASessionUpdateFailedMessage() {
        gateway.result = presentSession(armedSnapshot(), emptyList()).copy(extras = sessionExtras())
        val viewModel = viewModel()
        coordinator.result = AlarmSoundUpdateResult.DeferredUntilUnlock

        viewModel.onRingtoneSelected("niumi_piano")

        assertThat(viewModel.state.message).isEqualTo(RingtoneTexts.SESSION_UPDATE_FAILED)
    }

    @Test
    fun refreshLeavesTheArmedModeWhenTheSessionLeftArmedMeanwhile() {
        gateway.result = presentSession(armedSnapshot(), emptyList()).copy(extras = sessionExtras())
        val viewModel = viewModel()
        assertThat(viewModel.state.isSessionArmed).isTrue()

        gateway.result = presentSession(armedSnapshot(SessionStateDto.RINGING), emptyList())
        viewModel.refresh()

        assertThat(viewModel.state.isSessionArmed).isFalse()
        assertThat(viewModel.state.settings).isEqualTo(PREFERENCE_SETTINGS)
    }

    // Pré-écoute.

    @Test
    fun togglingAPreviewPlaysItThenStopsOnTheSameKey() {
        gateway.result = LoadResult.Absent
        val viewModel = viewModel()

        viewModel.onPreviewToggled("niumi_bell")
        assertThat(viewModel.state.previewingKey).isEqualTo("niumi_bell")

        viewModel.onPreviewToggled("niumi_bell")
        assertThat(viewModel.state.previewingKey).isNull()
    }

    @Test
    fun previewingAnotherKeySwitchesTheOneReported() {
        gateway.result = LoadResult.Absent
        val viewModel = viewModel()

        viewModel.onPreviewToggled("niumi_bell")
        viewModel.onPreviewToggled("niumi_oiseaux")

        assertThat(viewModel.state.previewingKey).isEqualTo("niumi_oiseaux")
    }

    @Test
    fun theNaturalEndOfThePreviewClearsTheReportedKeyWithoutAnyAction() {
        gateway.result = LoadResult.Absent
        val viewModel = viewModel()
        viewModel.onPreviewToggled("niumi_bell")

        previewPlayer.completeNaturally()

        assertThat(viewModel.state.previewingKey).isNull()
    }

    @Test
    fun onPauseStopsAnyOngoingPreview() {
        gateway.result = LoadResult.Absent
        val viewModel = viewModel()
        viewModel.onPreviewToggled("niumi_bell")

        viewModel.onPause()

        assertThat(viewModel.state.previewingKey).isNull()
    }

    @Test
    fun aZeroAlarmVolumeRefusesThePreviewWithoutCallingThePlayer() {
        gateway.result = LoadResult.Absent
        volumeSource.volume = 0
        val viewModel = viewModel()

        viewModel.onPreviewToggled("niumi_bell")

        assertThat(viewModel.state.isAlarmVolumeZero).isTrue()
        assertThat(previewPlayer.playCalls).isEqualTo(0)
        assertThat(viewModel.state.previewingKey).isNull()
    }

    @Test
    fun aFailedPreviewShowsAMessage() {
        gateway.result = LoadResult.Absent
        previewPlayer.failingKey = "niumi_bell"
        val viewModel = viewModel()

        viewModel.onPreviewToggled("niumi_bell")

        assertThat(viewModel.state.message).isEqualTo(RingtoneTexts.PREVIEW_FAILED)
    }

    @Test
    fun anUnknownRingtoneSelectionIsSanitizedToTheDefaultKey() {
        gateway.result = LoadResult.Absent
        val viewModel = viewModel()

        viewModel.onRingtoneSelected("niumi_alarm")

        assertThat(viewModel.state.settings.ringtoneKey).isEqualTo(NiumiRingtones.DEFAULT_KEY)
    }
}
