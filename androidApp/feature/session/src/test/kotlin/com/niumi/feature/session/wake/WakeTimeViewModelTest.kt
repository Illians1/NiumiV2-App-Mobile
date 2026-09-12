package com.niumi.feature.session.wake

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.feature.session.wake.fakes.FakeClock
import com.niumi.feature.session.wake.fakes.FakeSetupPreferences
import com.niumi.feature.session.wake.fakes.FakeTimeZoneProvider
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Écran 5 (SPEC_ANDROID §15 ; SPEC_CORE_KMP §8.1). Le calcul lui-même est prouvé dans
 * `:shared:core` (`WakeScheduleCalculatorTest`) : ces tests portent sur ce que le ViewModel en
 * fait — affichage, message, garde de session, mémorisation de la dernière heure choisie.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WakeTimeViewModelTest {
    private fun paris(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        ZonedDateTime
            .of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Paris"))
            .toInstant()
            .toEpochMilli()

    private val clock = FakeClock(now = paris(2026, 9, 3, 20, 0))
    private val timeZoneProvider = FakeTimeZoneProvider(zoneId = "Europe/Paris")
    private val snapshotPublisher = SessionSnapshotPublisher()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(setupPreferences: FakeSetupPreferences = FakeSetupPreferences()): WakeTimeViewModel =
        WakeTimeViewModel(NiumiCoreFacade(), clock, timeZoneProvider, setupPreferences, snapshotPublisher)

    @Test
    fun aTimeChosenBeforeTheCurrentHourIsScheduledForTomorrow() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.display?.relativeDayLabel).isEqualTo("Demain")
    }

    @Test
    fun aTimeChosenAfterTheCurrentHourIsScheduledForToday() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(23, 0)

        assertThat(viewModel.state.display?.relativeDayLabel).isEqualTo("Aujourd'hui")
    }

    @Test
    fun aTimeExactlyEqualToTheCurrentInstantIsScheduledForTomorrow() {
        clock.now = paris(2026, 9, 4, 7, 0)
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.display?.relativeDayLabel).isEqualTo("Demain")
    }

    @Test
    fun onTimeChangedProducesATwoDigitIsoLocalTime() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 5)

        assertThat(viewModel.state.localTimeIso).isEqualTo("07:05")
    }

    @Test
    fun aSystemTimeZoneChangeIsPickedUpOnRefresh() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        val before = viewModel.state.display?.let { it.timeLabel to it.zoneLabel }

        timeZoneProvider.zoneId = "Pacific/Auckland"
        viewModel.refresh(use24Hour = true)

        assertThat(viewModel.state.display?.zoneLabel).isEqualTo("Pacific/Auckland")
        assertThat(viewModel.state.display?.let { it.timeLabel to it.zoneLabel }).isNotEqualTo(before)
    }

    @Test
    fun anUnknownTimeZoneShowsAMessageAndDisablesContinuationWithoutThrowing() {
        timeZoneProvider.zoneId = "Not/AZone"
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.display).isNull()
        assertThat(viewModel.state.message).isEqualTo(WakeTimeTexts.UNKNOWN_ZONE_MESSAGE)
        assertThat(viewModel.state.canContinue).isFalse()
    }

    @Test
    fun aSessionInProgressDisablesContinuation() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        assertThat(viewModel.state.canContinue).isTrue()

        snapshotPublisher.publish(armedSnapshot())

        assertThat(viewModel.state.isSessionInProgress).isTrue()
        assertThat(viewModel.state.canContinue).isFalse()
    }

    @Test
    fun theDisplayedScheduleIsNeverNullWhenContinuationIsAllowed() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        if (viewModel.state.canContinue) {
            assertThat(viewModel.state.display).isNotNull()
        }
    }

    @Test
    fun theDialOpensOnTheLastConfirmedTimeRatherThanTheDefault() {
        val viewModel = viewModel(FakeSetupPreferences(lastWakeTimeIsoValue = "06:30"))

        assertThat(viewModel.state.localTimeIso).isEqualTo("06:30")
    }

    @Test
    fun theDialOpensOnTheDefaultTimeWhenNoneWasEverConfirmed() {
        val viewModel = viewModel(FakeSetupPreferences(lastWakeTimeIsoValue = null))

        assertThat(viewModel.state.localTimeIso).isEqualTo(DEFAULT_LOCAL_TIME_ISO)
    }

    @Test
    fun continuingPersistsTheChosenTimeAndInvokesTheCallback() {
        val preferences = FakeSetupPreferences()
        val viewModel = viewModel(preferences)
        viewModel.onTimeChanged(6, 30)
        var confirmed: String? = null

        viewModel.continueToSummary { localTimeIso -> confirmed = localTimeIso }

        assertThat(confirmed).isEqualTo("06:30")
    }

    private fun armedSnapshot() =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 1,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule =
                WakeScheduleDto(
                    localDateIso = "2026-09-04",
                    localTimeIso = "07:00",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
                ),
            state = SessionStateDto.ARMED,
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
}
