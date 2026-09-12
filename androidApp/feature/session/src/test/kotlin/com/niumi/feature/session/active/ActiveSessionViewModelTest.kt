package com.niumi.feature.session.active

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.feature.session.wake.fakes.FakeClock
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
 * Écran 7, version minimale de l'étape 14 (SPEC_ANDROID §15, §8). L'instant programmé ne change
 * jamais après l'armement : seule sa lecture locale suit le fuseau courant.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModelTest {
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

    private val now = paris(2026, 9, 3, 20, 0)
    private val clock = FakeClock(now)
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

    private fun viewModel() = ActiveSessionViewModel(clock, timeZoneProvider, snapshotPublisher)

    private fun snapshot(state: SessionStateDto = SessionStateDto.ARMED) =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 2,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule =
                WakeScheduleDto(
                    localDateIso = "2026-09-04",
                    localTimeIso = "07:00",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
                ),
            state = state,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = now,
            armedAtEpochMillis = now,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    @Test
    fun anArmedSnapshotShowsItsStateDateTimeAndZone() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.state).isEqualTo(SessionStateDto.ARMED)
        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("07:00")
        assertThat(viewModel.state.displayAtActivation?.zoneLabel).isEqualTo("Europe/Paris")
        assertThat(viewModel.state.hasSession).isTrue()
    }

    @Test
    fun sameZoneDoesNotAddASecondReading() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.displayInCurrentZone).isNull()
    }

    @Test
    fun aDifferentCurrentZoneAddsARecomputedLocalReadingWithoutChangingTheInstant() {
        timeZoneProvider.zoneId = "Pacific/Auckland"
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("07:00")
        assertThat(viewModel.state.displayInCurrentZone?.zoneLabel).isEqualTo("Pacific/Auckland")
        assertThat(viewModel.state.displayInCurrentZone?.timeLabel).isEqualTo("17:00")
    }

    /**
     * Défaut mesuré sur appareil à l'étape 14 : l'écran 7 affichait « 07:00 » sur un téléphone
     * réglé en 12 h, alors que les écrans 5 et 6 affichaient « 7:00 AM ». Trois écrans portant la
     * même information ne peuvent pas employer deux conventions (§15).
     */
    @Test
    fun aTwelveHourDeviceRendersAmPmOnTheActiveSessionScreen() {
        val viewModel = viewModel()

        viewModel.refresh(use24Hour = false)
        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("7:00 AM")
    }

    @Test
    fun switchingToTwelveHourReprojectsTheSnapshotAlreadyPublished() {
        val viewModel = viewModel()
        snapshotPublisher.publish(snapshot())
        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("07:00")

        viewModel.refresh(use24Hour = false)

        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("7:00 AM")
    }

    @Test
    fun noSnapshotShowsTheEmptyStateWithoutCrashing() {
        val viewModel = viewModel()

        assertThat(viewModel.state.hasSession).isFalse()
        assertThat(viewModel.state.isLoading).isFalse()
    }

    @Test
    fun everySessionStateHasALabel() {
        SessionStateDto.entries.forEach { state ->
            assertThat(ActiveSessionTexts.stateLabel(state)).isNotEmpty()
        }
    }
}
