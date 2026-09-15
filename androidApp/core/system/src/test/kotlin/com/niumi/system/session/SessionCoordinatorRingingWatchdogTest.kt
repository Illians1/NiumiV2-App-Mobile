package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Alarme de secours pendant `RINGING`, câblée sur les effets du coordinateur (SPEC_ANDROID §10.2,
 * §9.1 ; étape 20) : `START_RINGING` l'arme, `STOP_RINGING` la désarme avant que la session ne se
 * ferme. [SessionReconcilerWatchdogTest] prouve le même contrat côté réconciliateur.
 */
class SessionCoordinatorRingingWatchdogTest {
    private suspend fun armSession(harness: TestCoordinatorHarness): SessionSnapshotDto {
        val result =
            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(eventId = "00000000-0000-0000-0000-000000000001"),
                SessionDtoFixtures.extras(),
            )
        return (result as DispatchResult.Applied).snapshot!!
    }

    @Test
    fun alarmFiredArmsTheWatchdog() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)

            val result = harness.coordinator.dispatch(harness.eventFactory.alarmFired(armed))

            assertThat((result as DispatchResult.Applied).snapshot?.state).isEqualTo(SessionStateDto.RINGING)
            assertThat(harness.ringingWatchdog.armed).containsExactly(SessionDtoFixtures.SESSION_ID)
        }

    @Test
    fun aValidScanDisarmsTheWatchdogBeforeTheSessionCloses() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)
            val ringing =
                (harness.coordinator.dispatch(harness.eventFactory.alarmFired(armed)) as DispatchResult.Applied)
                    .snapshot!!
            assertThat(harness.ringingWatchdog.armed).isNotEmpty()

            val scan =
                SessionDtoFixtures.validNfcScanned(
                    facade = harness.facade,
                    eventId = "00000000-0000-0000-0000-000000000002",
                    expectedRevision = ringing.revision,
                    occurredAtEpochMillis = (ringing.ringingAtEpochMillis ?: 0L) + 1,
                )

            harness.coordinator.dispatch(scan)

            assertThat(harness.ringingWatchdog.armed).isEmpty()
        }
}
