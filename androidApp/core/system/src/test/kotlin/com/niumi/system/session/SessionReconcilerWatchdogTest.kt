package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Alarme de secours pendant `RINGING` (SPEC_ANDROID §10.2, §9.1 ; étape 20). Complète
 * [SessionReconcilerRingingTest] : celui-ci prouve que le son revient, celui-ci prouve que la
 * réconciliation applique la politique du watchdog — l'armer tant que la session sonne, le
 * désarmer sinon — à chaque passe.
 */
class SessionReconcilerWatchdogTest {
    private suspend fun TestCoordinatorHarness.persist(state: SessionStateDto) {
        gateway.commit(
            StoredDecision(
                snapshot = SessionDtoFixtures.snapshotInState(state, revision = 3),
                receipt =
                    EventReceipt(
                        eventId = "00000000-0000-0000-0000-000000000009",
                        sessionId = SessionDtoFixtures.SESSION_ID,
                        payloadSha256Hex = "c".repeat(64),
                        appliedRevision = 3,
                        receivedAtEpochMillis = 1_000L,
                    ),
                effects = emptyList(),
                androidExtras = SessionDtoFixtures.extras(),
            ),
        )
    }

    /** Le watchdog est réarmé **après** que le son a repris, jamais avant. */
    @Test
    fun aRingingSessionRearmsTheWatchdogAfterTheSoundResumes() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persist(SessionStateDto.RINGING)

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.ringingWatchdog.armed).containsExactly(SessionDtoFixtures.SESSION_ID)
            val startIndex = harness.journal.calls.indexOf("RingingController.startRinging")
            val armIndex = harness.journal.calls.indexOf("RingingWatchdog.arm")
            assertThat(startIndex).isAtLeast(0)
            assertThat(armIndex).isGreaterThan(startIndex)
        }

    @Test
    fun everyOtherStateDisarmsTheWatchdog() =
        runTest {
            listOf(
                SessionStateDto.PREPARING,
                SessionStateDto.ARMED,
                SessionStateDto.AWAITING_NFC,
                SessionStateDto.TRIGGERED_AWAITING_NFC,
                SessionStateDto.RELEASING,
            ).forEach { state ->
                val harness = TestCoordinatorHarness()
                harness.persist(state)

                harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

                assertThat(harness.ringingWatchdog.armed).isEmpty()
                assertThat(harness.journal.calls).contains("RingingWatchdog.disarm")
            }
        }

    /** Réarmer deux passes de suite sur `RINGING` reste un seul identifiant armé. */
    @Test
    fun rearmingTwiceIsIdempotent() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persist(SessionStateDto.RINGING)

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)
            harness.coordinator.reconcile(ReconcileReason.RINGING_WATCHDOG)

            assertThat(harness.ringingWatchdog.armed).containsExactly(SessionDtoFixtures.SESSION_ID)
        }

    /** `RINGING_WATCHDOG` ne fusionne pas Direct Boot et ne reprogramme pas l'alarme du réveil. */
    @Test
    fun theWatchdogReasonNeitherMergesDirectBootNorReschedulesTheWakeAlarm() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persist(SessionStateDto.RINGING)

            harness.coordinator.reconcile(ReconcileReason.RINGING_WATCHDOG)

            assertThat(harness.journal.calls).doesNotContain("AlarmScheduler.schedule")
            assertThat(harness.roomMerge.merged).isEmpty()
        }
}
