package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import com.niumi.system.session.fakes.persistSession
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Reprise d'un nettoyage interrompu et republication de la demande de scan (SPEC_ANDROID §11.3
 * dernier alinéa, §10.5, §18).
 *
 * Ces trois états — `RELEASING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` — étaient les seuls que
 * `SessionReconciler` traversait sans rien faire. Une mort de processus entre le scan et l'état
 * final y laissait des applications bloquées sans aucun chemin de sortie : l'écran de réveil est
 * fermé, l'alarme est annulée, et `RELEASE_SUCCEEDED` n'a jamais été envoyé.
 */
class SessionReconcilerReleasingTest {
    /**
     * Dernier état publié à l'interface. Un état final efface le pointeur actif
     * (`CLEAR_ACTIVE_SESSION`, §11.3 point 14) : la persistance ne peut donc pas en témoigner.
     */
    private fun TestCoordinatorHarness.publishedState(): SessionStateDto? = publisher.snapshot.value?.state

    private fun pendingRemoveBlocking(
        revision: Long = 4,
        ordinal: Int = 0,
        status: EffectStatus = EffectStatus.FAILED,
    ) = PendingEffect(
        effectId = "${SessionDtoFixtures.SESSION_ID}:$revision:REMOVE_BLOCKING:$ordinal",
        sessionId = SessionDtoFixtures.SESSION_ID,
        revision = revision,
        kind = SessionEffectKindDto.REMOVE_BLOCKING,
        ordinal = ordinal,
        payloadJson = null,
        status = status,
        lastError = "REMOVE_FAILED",
    )

    private suspend fun TestCoordinatorHarness.persistReleasing(effects: List<PendingEffect>) =
        persistSession(
            SessionStateDto.RELEASING,
            revision = 4,
            effects = effects,
            releaseTarget = ReleaseTargetDto.COMPLETED,
        )

    // --- RELEASING ------------------------------------------------------------------------------

    /**
     * Le cas nominal de reprise : l'effet requis resté en échec réussit au rejeu, la phase se
     * referme. C'est ce que §11.3 appelle « reprendre uniquement les effets manquants ».
     */
    @Test
    fun aReleasingSessionClosesOnceTheMissingEffectSucceeds() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistReleasing(listOf(pendingRemoveBlocking()))

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.publishedState()).isEqualTo(SessionStateDto.COMPLETED)
        }

    /**
     * §11.3 : « sans réappliquer un blocage déjà retiré ». Le rejeu ne connaît que l'outbox, et
     * `APPLY_BLOCKING` y est `SUCCEEDED` depuis l'activation — donc hors du champ de `replayable`.
     */
    @Test
    fun resumingNeverReappliesBlocking() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistReleasing(listOf(pendingRemoveBlocking()))

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.journal.calls).doesNotContain("BlockingController.apply")
            assertThat(harness.journal.calls).contains("BlockingController.remove")
        }

    /**
     * Processus mort entre la persistance de la décision et l'envoi de `RELEASE_SUCCEEDED` : tous
     * les effets avaient réussi, l'outbox est vide, et pourtant la session reste `RELEASING`.
     * Sans cette branche, rien ne la termine jamais.
     */
    @Test
    fun anEmptyOutboxStillClosesTheRelease() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistReleasing(effects = emptyList())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.publishedState()).isEqualTo(SessionStateDto.COMPLETED)
        }

    /**
     * « L'application ne présente pas la session comme terminée avant `RELEASE_SUCCEEDED` » : un
     * effet requis toujours en échec conserve `RELEASING`, sans nouvel incident — le coordinateur
     * a déjà consigné `RELEASE_PARTIAL_FAILURE` au moment de l'échec, le répéter à chaque passe
     * rejouerait les doublons corrigés à l'étape 16.
     */
    @Test
    fun aStillFailingRequiredEffectKeepsReleasing() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.blockingController.removeResult = OperationResult.Failure("REMOVE_FAILED")
            harness.persistReleasing(listOf(pendingRemoveBlocking()))

            val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(result.actions).contains(ReconcileAction.ReleaseStillPending(effectCount = 1))
            assertThat((harness.gateway.load() as LoadResult.Present).snapshot.state)
                .isEqualTo(SessionStateDto.RELEASING)
            assertThat(harness.gateway.incidentsRecorded).isEmpty()
        }

    /**
     * Un effet best-effort resté en échec ne retient pas la phase (SPEC_CORE_KMP §6, dernier
     * alinéa) : seuls `CANCEL_ALARM`, `STOP_RINGING` et `REMOVE_BLOCKING` sont requis.
     */
    @Test
    fun aFailingBestEffortEffectDoesNotHoldTheRelease() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.scanRequestNotifier.clearResult = OperationResult.Failure("NOTIFICATION_FAILED")
            harness.persistReleasing(
                listOf(
                    PendingEffect(
                        effectId = "${SessionDtoFixtures.SESSION_ID}:4:CLEAR_SCAN_REQUEST:0",
                        sessionId = SessionDtoFixtures.SESSION_ID,
                        revision = 4,
                        kind = SessionEffectKindDto.CLEAR_SCAN_REQUEST,
                        ordinal = 0,
                        payloadJson = null,
                        status = EffectStatus.FAILED,
                        lastError = "NOTIFICATION_FAILED",
                    ),
                ),
            )

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.publishedState()).isEqualTo(SessionStateDto.COMPLETED)
        }

    /**
     * Arbitrage du 2026-09-14 : la reprise ne filtre sur aucune [ReconcileReason]. Le plan la
     * limitait à `PROCESS_START`, `USER_UNLOCKED` et `SERVICE_RECREATED`, mais l'étape 19 réconcilie
     * un redémarrage sous `BOOT` — un appareil qui redémarre pendant le nettoyage serait alors
     * resté bloqué.
     */
    @Test
    fun everyReasonResumesTheRelease() =
        runTest {
            ReconcileReason.entries.forEach { reason ->
                val harness = TestCoordinatorHarness()
                harness.persistReleasing(effects = emptyList())

                harness.coordinator.reconcile(reason)

                assertThat(harness.publishedState()).isEqualTo(SessionStateDto.COMPLETED)
            }
        }

    // --- AWAITING_NFC / TRIGGERED_AWAITING_NFC --------------------------------------------------

    /**
     * §10.5 : la notification d'attente de scan est le seul rappel visible une fois l'écran de
     * réveil fermé. Publiée par `PRESENT_SCAN_REQUEST`, elle disparaît avec le processus sur
     * certaines surcouches ; la réconciliation la remet.
     */
    @Test
    fun scanAwaitingStatesRepublishTheScanRequest() =
        runTest {
            listOf(SessionStateDto.AWAITING_NFC, SessionStateDto.TRIGGERED_AWAITING_NFC).forEach { state ->
                val harness = TestCoordinatorHarness()
                harness.persistSession(state)

                val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

                assertThat(result.actions).contains(ReconcileAction.ScanRequestRepublished)
                assertThat(harness.scanRequestNotifier.presentCallCount).isEqualTo(1)
            }
        }

    /**
     * `present()` est idempotent par identifiant de notification : republier remplace en place.
     * Aucune décision n'est prise, donc aucune révision n'avance — c'est ce qui garantit l'absence
     * de « doublon visible ».
     */
    @Test
    fun republishingTwiceTakesNoDecision() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.AWAITING_NFC, revision = 3)

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)
            val second = harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(harness.scanRequestNotifier.presentCallCount).isEqualTo(2)
            assertThat(second.actions.filterIsInstance<ReconcileAction.DecisionApplied>()).isEmpty()
            assertThat((harness.gateway.load() as LoadResult.Present).snapshot.revision).isEqualTo(3)
        }

    /** Une session en attente de scan ne sonne pas et ne se termine pas toute seule (§10.4). */
    @Test
    fun scanAwaitingStatesNeitherRingNorRelease() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.TRIGGERED_AWAITING_NFC)

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.journal.calls).doesNotContain("RingingController.startRinging")
            assertThat((harness.gateway.load() as LoadResult.Present).snapshot.state)
                .isEqualTo(SessionStateDto.TRIGGERED_AWAITING_NFC)
        }

    // --- Non-régression du rejeu d'outbox -------------------------------------------------------

    /**
     * Deux incidents collectés dans une même passe de rejeu doivent produire **deux** décisions.
     * La boucle construisait chaque `INCIDENT_REPORTED` sur le snapshot d'entrée : le second
     * partait donc avec une révision déjà consommée et tombait en `STALE_REVISION`. Même classe de
     * défaut que celui mesuré sur appareil à l'étape 17 (essai 7), corrigé alors dans
     * `SessionReadinessMonitor` mais pas ici.
     */
    @Test
    fun twoIncidentsInOneReplayBothReachTheEngine() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.blockingController.removeResult = OperationResult.AlreadySatisfied
            harness.blockingController.serviceEnabled = false
            harness.persistSession(
                SessionStateDto.TRIGGERED_AWAITING_NFC,
                revision = 4,
                effects =
                    listOf(
                        pendingRemoveBlocking(revision = 3, ordinal = 0),
                        pendingRemoveBlocking(revision = 4, ordinal = 0),
                    ),
            )

            val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            val applied =
                result.actions
                    .filterIsInstance<ReconcileAction.DecisionApplied>()
                    .map { it.dispatchResult }
            assertThat(applied).hasSize(2)
            assertThat(applied.filterIsInstance<DispatchResult.Rejected>()).isEmpty()
        }
}
