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
 * Reprise de la sonnerie après une mort de processus (SPEC_ANDROID §10.2).
 *
 * `START_STICKY` est censé relancer le service, qui se reconstruit alors depuis le snapshot. **La
 * plateforme ne le fait pas toujours** : mesuré sur appareil à l'étape 17, HyperOS n'a rejoué aucun
 * redémarrage après un crash, et la sonnerie s'est arrêtée définitivement alors que la session
 * restait active. La réconciliation est le second filet.
 */
class SessionReconcilerRingingTest {
    private suspend fun TestCoordinatorHarness.persistRinging(state: SessionStateDto) {
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

    /**
     * Le rejeu de l'outbox ne rattrape pas ce cas : `START_RINGING` y est déjà `SUCCEEDED`. Sans
     * cette reprise, aucun chemin ne ranime le son — pas même ouvrir l'application.
     */
    @Test
    fun aRingingSessionGetsItsSoundBackAtProcessStart() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistRinging(SessionStateDto.RINGING)

            val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(result.actions).contains(ReconcileAction.RingingResumed)
            assertThat(harness.journal.calls).contains("RingingController.startRinging")
        }

    /** L'appel est idempotent : le relancer alors que le service tourne déjà est sans effet. */
    @Test
    fun resumingTwiceIsHarmless() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistRinging(SessionStateDto.RINGING)

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)
            harness.coordinator.reconcile(ReconcileReason.SERVICE_RECREATED)

            assertThat(harness.journal.calls.count { it == "RingingController.startRinging" })
                .isEqualTo(2)
            assertThat((harness.gateway.load() as LoadResult.Present).snapshot.state)
                .isEqualTo(SessionStateDto.RINGING)
        }

    /**
     * Les trois autres états non finaux ne sonnent pas : `AWAITING_NFC` et
     * `TRIGGERED_AWAITING_NFC` attendent un scan sans audio (§10.4), et `RELEASING` a déjà été
     * scanné. Leur reprise relève de l'étape 18.
     */
    @Test
    fun theScanAwaitingAndReleasingStatesNeverStartTheSound() =
        runTest {
            listOf(
                SessionStateDto.AWAITING_NFC,
                SessionStateDto.TRIGGERED_AWAITING_NFC,
                SessionStateDto.RELEASING,
            ).forEach { state ->
                val harness = TestCoordinatorHarness()
                harness.persistRinging(state)

                val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

                assertThat(result.actions).doesNotContain(ReconcileAction.RingingResumed)
                assertThat(harness.journal.calls).doesNotContain("RingingController.startRinging")
            }
        }
}
