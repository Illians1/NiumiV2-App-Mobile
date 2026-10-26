package com.niumi.feature.session.success

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.ReleaseMeansDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.feature.session.fakes.FakeTimeZoneProvider
import com.niumi.system.insight.TimeToGetUpPreferences
import com.niumi.system.session.SessionHistoryReader
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
 * Écran 11 (`SPECIFICATION.md` §15, étape 46) : l'heure du lever est celle du scan, le délai part
 * de la sonnerie réelle, « Bravo ! » seulement si l'utilisateur s'est levé plus vite que la durée
 * déclarée. Sans sonnerie, ni délai ni « Bravo ! » ; après la recopie du matin, pas de « Bravo ! »
 * (D2) — décisions de Mehdi du 2026-10-24 et du 2026-10-25.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SuccessViewModelTest {
    private val history = mutableMapOf<String, SessionSnapshotDto>()
    private val preferences = FakeTimeToGetUp()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        SuccessViewModel(
            historyReader = SessionHistoryReader { history[it] },
            timeToGetUp = preferences,
            facade = NiumiCoreFacade(),
            timeZoneProvider = FakeTimeZoneProvider(PARIS),
        )

    private fun paris(time: String): Long =
        ZonedDateTime.parse("2026-10-26T$time:00+01:00[$PARIS]").toInstant().toEpochMilli()

    private fun completed(
        ringingAt: Long? = paris("07:00"),
        releasedAt: Long = paris("07:04"),
        releasedBy: ReleaseMeansDto = ReleaseMeansDto.NFC_SCAN,
        state: SessionStateDto = SessionStateDto.COMPLETED,
    ) = SessionSnapshotDto(
        schemaVersion = 3,
        revision = 9,
        sessionId = SESSION_ID,
        wakeSchedule = WakeScheduleDto("2026-10-26", "07:00", PARIS, paris("07:00")),
        state = state,
        releaseTarget = null,
        health = SessionHealthDto.HEALTHY,
        createdAtEpochMillis = 1_000L,
        armedAtEpochMillis = 1_100L,
        ringingAtEpochMillis = ringingAt,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = null,
        nfcVerifiedAtEpochMillis = releasedAt.takeIf { releasedBy == ReleaseMeansDto.NFC_SCAN },
        releasingAtEpochMillis = releasedAt,
        completedAtEpochMillis = releasedAt + 300,
        cancelledAtEpochMillis = null,
        failureCode = null,
        releasedBy = releasedBy,
    )

    private fun shown(): SuccessUiState.Shown {
        val viewModel = viewModel()
        viewModel.load(SESSION_ID)
        return viewModel.state as SuccessUiState.Shown
    }

    /** L'exemple de la maquette 42 : sonnerie à 7:00, scan à 7:04, 15 min déclarées. */
    @Test
    fun theExampleOfTheMockup() {
        history[SESSION_ID] = completed()
        preferences.minutes = 15

        val shown = shown()

        assertThat(shown.title).isEqualTo("Debout à 7:04.")
        assertThat(shown.afterAlarm).isEqualTo("4 minutes après la sonnerie.")
        assertThat(shown.savedComparison).isEqualTo("C’est 11 minutes de moins qu’avant Niumi.")
    }

    @Test
    fun slowerThanTheDeclaredDurationShowsNoBravo() {
        history[SESSION_ID] = completed(releasedAt = paris("07:20"))
        preferences.minutes = 15

        val shown = shown()

        assertThat(shown.afterAlarm).isEqualTo("20 minutes après la sonnerie.")
        assertThat(shown.savedComparison).isNull()
    }

    /** Avant l'étape 50, aucune durée n'est déclarée : jamais de « Bravo ! ». */
    @Test
    fun withoutADeclaredDurationThereIsNoBravo() {
        history[SESSION_ID] = completed()

        val shown = shown()

        assertThat(shown.afterAlarm).isEqualTo("4 minutes après la sonnerie.")
        assertThat(shown.savedComparison).isNull()
    }

    /** Décision 2 du 2026-10-25 : l'alarme n'a pas sonné (téléphone éteint), rien ne se compte. */
    @Test
    fun withoutRingingThereIsNeitherDelayNorBravo() {
        history[SESSION_ID] = completed(ringingAt = null, releasedAt = paris("09:00"))
        preferences.minutes = 15

        val shown = shown()

        assertThat(shown.title).isEqualTo("Debout à 9:00.")
        assertThat(shown.afterAlarm).isNull()
        assertThat(shown.savedComparison).isNull()
    }

    /** Une alarme partie en retard compte depuis la sonnerie réelle. */
    @Test
    fun aLateAlarmCountsFromTheActualRinging() {
        history[SESSION_ID] = completed(ringingAt = paris("07:20"), releasedAt = paris("07:22"))

        assertThat(shown().afterAlarm).isEqualTo("2 minutes après la sonnerie.")
    }

    /** D2 : la recopie du matin mène à l'écran 11, sans « Bravo ! ». */
    @Test
    fun theMorningEmergencyExitShowsNoBravo() {
        history[SESSION_ID] = completed(releasedBy = ReleaseMeansDto.EMERGENCY_EXIT)
        preferences.minutes = 15

        val shown = shown()

        assertThat(shown.title).isEqualTo("Debout à 7:04.")
        assertThat(shown.savedComparison).isNull()
    }

    @Test
    fun anUnknownSessionLeavesForHome() {
        val viewModel = viewModel()

        viewModel.load(SESSION_ID)

        assertThat(viewModel.state).isEqualTo(SuccessUiState.NotFound)
    }

    @Test
    fun aSessionThatIsNotCompletedLeavesForHome() {
        history[SESSION_ID] = completed(state = SessionStateDto.CANCELLED)
        val viewModel = viewModel()

        viewModel.load(SESSION_ID)

        assertThat(viewModel.state).isEqualTo(SuccessUiState.NotFound)
    }

    private class FakeTimeToGetUp : TimeToGetUpPreferences {
        var minutes: Int? = null

        override suspend fun declaredMinutes(): Int? = minutes

        override suspend fun setDeclaredMinutes(value: Int) = error("WRITE_FROM_SUCCESS_SCREEN")
    }

    private companion object {
        const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
        val PARIS: String = ZoneId.of("Europe/Paris").id
    }
}
