package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.setup.SetupPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

private class FakeSetupPreferences(
    private var acknowledged: Boolean = true,
) : SetupPreferences {
    override suspend fun isOnboardingAcknowledged(): Boolean = acknowledged

    override suspend fun acknowledgeOnboarding() {
        acknowledged = true
    }

    override suspend fun isBatteryExemptionConfirmed(): Boolean = true

    override suspend fun setBatteryExemptionConfirmed(confirmed: Boolean) = Unit

    override suspend fun lastWakeTimeIso(): String? = null

    override suspend fun setLastWakeTimeIso(value: String) = Unit
}

/**
 * Redirection de l'accueil vers l'écran 8 pendant la sonnerie (SPEC_ANDROID §10.4, étape 17), et
 * sa garde anti-boucle : « L'utilisateur peut écarter l'écran volontairement ; il ne doit jamais
 * perdre tout accès au scan. »
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val publisher = SessionSnapshotPublisher()
    private val preferences = FakeSetupPreferences()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun snapshot(state: SessionStateDto) =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 2,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule = WakeScheduleDto("2026-09-13", "07:00", "Europe/Paris", 2_000_000_000_000L),
            state = state,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1_000L,
            armedAtEpochMillis = 1_100L,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    private fun viewModel() = HomeViewModel(preferences, publisher)

    @Test
    fun aRingingSessionAsksForTheAlarmScreen() =
        runTest {
            publisher.publish(snapshot(SessionStateDto.RINGING))

            assertThat(viewModel().state.pendingAlarmScreen).isTrue()
        }

    @Test
    fun anArmedSessionKeepsTheActiveSessionScreen() =
        runTest {
            publisher.publish(snapshot(SessionStateDto.ARMED))

            val state = viewModel().state

            assertThat(state.pendingAlarmScreen).isFalse()
            assertThat(state.alarmScreenRequired).isFalse()
            assertThat(state.destination).isEqualTo(NiumiRoute.ActiveSession)
            assertThat(state.hasActiveSession).isTrue()
        }

    /** La garde n'empêche qu'un second lancement dans le même passage au premier plan. */
    @Test
    fun theAlarmScreenIsNotLaunchedTwiceWithinTheSameResume() =
        runTest {
            publisher.publish(snapshot(SessionStateDto.RINGING))
            val viewModel = viewModel()

            viewModel.onAlarmScreenOpened()

            assertThat(viewModel.state.pendingAlarmScreen).isFalse()
            // Le besoin, lui, ne disparaît pas : le bouton principal y mène toujours.
            assertThat(viewModel.state.alarmScreenRequired).isTrue()
        }

    /**
     * §10.4, resserré à l'étape 17 après mesure sur appareil : tant que la session attend un scan,
     * **aucun autre écran de Niumi** n'est atteignable. Chaque retour au premier plan réarme donc
     * la redirection, au lieu d'y renoncer pour la vie de l'écran.
     */
    @Test
    fun everyReturnToTheForegroundSendsBackToTheAlarmScreen() =
        runTest {
            publisher.publish(snapshot(SessionStateDto.RINGING))
            val viewModel = viewModel()
            viewModel.onAlarmScreenOpened()

            viewModel.refresh()

            assertThat(viewModel.state.pendingAlarmScreen).isTrue()
        }

    @Test
    fun theFourScanStatesAllRequireTheAlarmScreen() =
        runTest {
            listOf(
                SessionStateDto.RINGING,
                SessionStateDto.AWAITING_NFC,
                SessionStateDto.TRIGGERED_AWAITING_NFC,
                SessionStateDto.RELEASING,
            ).forEach { state ->
                publisher.publish(snapshot(state))
                assertThat(viewModel().state.alarmScreenRequired).isTrue()
            }
        }

    @Test
    fun withoutASessionNothingIsRedirected() =
        runTest {
            val state = viewModel().state

            assertThat(state.pendingAlarmScreen).isFalse()
            assertThat(state.hasActiveSession).isFalse()
            assertThat(state.destination).isEqualTo(NiumiRoute.Readiness)
        }
}
