package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.domain.ViolationCode
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.isBlockingPending
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.ringing.ServiceCommandExtras
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Début du blocage différé par le coordinateur (SPEC_ANDROID §12.4 ; SPEC_CORE_KMP §5.1, §5.2, §6).
 * `BlockingStartReceiver` n'est qu'une coquille : toute la décision se prouve ici en JVM. Miroir
 * d'[AlarmTriggerHandlerTest], dont les gardes sont volontairement identiques.
 */
class BlockingStartHandlerTest {
    private val activationEventId = "00000000-0000-0000-0000-000000000001"

    private fun handlerFor(harness: TestCoordinatorHarness) =
        BlockingStartHandler(
            gateway = harness.gateway,
            coordinator = harness.coordinator,
            eventFactory = harness.eventFactory,
            technicalEventLog = harness.technicalEventLog,
        )

    /** Session réelle à blocage **différé**, armée par le coordinateur : `PREPARING` puis `ARMED`. */
    private suspend fun deferredHarness(): TestCoordinatorHarness {
        val harness = TestCoordinatorHarness()
        harness.coordinator.dispatch(
            SessionDtoFixtures.activationRequested(
                eventId = activationEventId,
                blockingStartsAtEpochMillis = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
            ),
            SessionDtoFixtures.extras(),
        )
        return harness
    }

    /** Session à blocage immédiat, celle qu'Android arme avant l'écran de l'étape 24. */
    private suspend fun immediateHarness(): TestCoordinatorHarness {
        val harness = TestCoordinatorHarness()
        harness.coordinator.dispatch(
            SessionDtoFixtures.activationRequested(eventId = activationEventId),
            SessionDtoFixtures.extras(),
        )
        return harness
    }

    private suspend fun TestCoordinatorHarness.currentSnapshot() = (gateway.load() as LoadResult.Present).snapshot

    private fun extrasFor(
        sessionId: String = SessionDtoFixtures.SESSION_ID,
        revision: Long,
    ) = ServiceCommandExtras(sessionId = sessionId, revision = revision)

    /**
     * Le cas nominal : à l'heure de début, le blocage est demandé et appliqué sans que l'état change.
     * `BLOCKING_STARTED` distingue au journal ce début différé d'un blocage posé à l'armement (§17).
     */
    @Test
    fun aDeferredSessionAppliesItsBlockingAndStaysArmed() =
        runTest {
            val harness = deferredHarness()
            val armed = harness.currentSnapshot()
            assertThat(armed.isBlockingPending).isTrue()
            harness.clock.now = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS

            val outcome = handlerFor(harness).handle(extrasFor(revision = armed.revision))

            assertThat(outcome).isInstanceOf(BlockingStartOutcome.Dispatched::class.java)
            val dispatched = (outcome as BlockingStartOutcome.Dispatched).result
            assertThat(dispatched).isInstanceOf(DispatchResult.Applied::class.java)
            val snapshot = (dispatched as DispatchResult.Applied).snapshot
            assertThat(snapshot?.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(snapshot?.blockingAppliedAtEpochMillis)
                .isEqualTo(SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS)
            assertThat(snapshot?.isBlockingPending).isFalse()
            assertThat(harness.journal.calls).contains("BlockingController.apply")
            assertThat(harness.technicalEventLog.entries)
                .contains(TechnicalEventType.BLOCKING_STARTED to SessionDtoFixtures.SESSION_ID)
        }

    /**
     * Garde de révision **monotone**, pas d'égalité : l'extra est figé au moment de
     * `SCHEDULE_BLOCKING_START`, et `INCIDENT_REPORTED` fait avancer la révision sans le réécrire.
     * Refuser une révision en retard ferait manquer le début précisément quand l'appareil est déjà
     * dégradé — même raisonnement qu'au réveil (§10.1).
     */
    @Test
    fun aRevisionOlderThanTheSnapshotStillStartsTheBlocking() =
        runTest {
            val harness = deferredHarness()
            val armedRevision = harness.currentSnapshot().revision
            harness.coordinator.dispatch(
                harness.eventFactory.incidentReported(
                    harness.currentSnapshot(),
                    harness.eventFactory.buildIncident(IncidentCodes.TIME_CHANGED, IncidentSeverityDto.WARNING),
                ),
            )
            assertThat(harness.currentSnapshot().revision).isGreaterThan(armedRevision)
            harness.clock.now = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS

            val outcome = handlerFor(harness).handle(extrasFor(revision = armedRevision))

            assertThat(outcome).isInstanceOf(BlockingStartOutcome.Dispatched::class.java)
            assertThat(harness.currentSnapshot().isBlockingPending).isFalse()
        }

    /** Révision en avance : la persistance est en retard sur ce qui a été programmé. */
    @Test
    fun aRevisionAheadOfTheSnapshotIsRefused() =
        runTest {
            val harness = deferredHarness()
            val revision = harness.currentSnapshot().revision
            harness.clock.now = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS

            val outcome = handlerFor(harness).handle(extrasFor(revision = revision + 1))

            assertThat(outcome).isEqualTo(BlockingStartOutcome.RevisionAhead)
            assertThat(harness.currentSnapshot().isBlockingPending).isTrue()
        }

    @Test
    fun anUnknownSessionIdIsRefused() =
        runTest {
            val harness = deferredHarness()
            val revision = harness.currentSnapshot().revision

            val outcome =
                handlerFor(
                    harness,
                ).handle(extrasFor(sessionId = SessionDtoFixtures.OTHER_SESSION_ID, revision = revision))

            assertThat(outcome).isEqualTo(BlockingStartOutcome.UnknownSession)
            assertThat(harness.currentSnapshot().isBlockingPending).isTrue()
        }

    /** Un `PendingIntent` qui survit à une session effacée n'applique aucun blocage. */
    @Test
    fun anAbsentSessionDispatchesNothing() =
        runTest {
            val harness = TestCoordinatorHarness()

            val outcome = handlerFor(harness).handle(extrasFor(revision = 2L))

            assertThat(outcome).isEqualTo(BlockingStartOutcome.NoSession)
            assertThat(harness.journal.calls).doesNotContain("BlockingController.apply")
        }

    /** SPEC_CORE_KMP §13 : un snapshot illisible ne se lit jamais « pas de session ». */
    @Test
    fun anUnreadableSnapshotDispatchesNothing() =
        runTest {
            val harness = deferredHarness()
            val revision = harness.currentSnapshot().revision
            harness.gateway.forceUnreadable = "SNAPSHOT_CORRUPTED"

            val outcome = handlerFor(harness).handle(extrasFor(revision = revision))

            assertThat(outcome).isEqualTo(BlockingStartOutcome.UnreadableSnapshot)
            harness.gateway.forceUnreadable = null
            assertThat(harness.currentSnapshot().isBlockingPending).isTrue()
        }

    @Test
    fun invalidExtrasAreRefusedWithoutTouchingTheSession() =
        runTest {
            val harness = deferredHarness()

            val outcome = handlerFor(harness).handle(ServiceCommandExtras(sessionId = null, revision = 2L))

            assertThat(outcome).isEqualTo(BlockingStartOutcome.InvalidCommand)
            assertThat(harness.currentSnapshot().isBlockingPending).isTrue()
        }

    /**
     * SPEC_ANDROID §12.4 : le handler ne vérifie ni l'état ni `blockingAppliedAtEpochMillis`, c'est le
     * moteur qui refuse (SPEC_CORE_KMP §5.2). Un second déclenchement est donc dispatché puis rejeté,
     * sans appliquer deux fois le blocage.
     */
    @Test
    fun aSecondTriggerIsRejectedByTheEngine() =
        runTest {
            val harness = deferredHarness()
            val revision = harness.currentSnapshot().revision
            harness.clock.now = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS
            handlerFor(harness).handle(extrasFor(revision = revision))
            val applyCalls = harness.journal.calls.count { it == "BlockingController.apply" }

            val outcome = handlerFor(harness).handle(extrasFor(revision = revision))

            val dispatched = (outcome as BlockingStartOutcome.Dispatched).result
            assertThat(dispatched).isInstanceOf(DispatchResult.Rejected::class.java)
            assertThat((dispatched as DispatchResult.Rejected).violations.map { it.code })
                .contains(ViolationCode.BLOCKING_ALREADY_APPLIED)
            assertThat(harness.journal.calls.count { it == "BlockingController.apply" }).isEqualTo(applyCalls)
        }

    /**
     * Déclenchement orphelin sur une session à blocage immédiat : le moteur refuse par
     * `BLOCKING_ALREADY_APPLIED`, son blocage ayant été demandé dès l'activation (§5.2).
     */
    @Test
    fun anImmediateSessionIsRejectedByTheEngine() =
        runTest {
            val harness = immediateHarness()
            val revision = harness.currentSnapshot().revision

            val outcome = handlerFor(harness).handle(extrasFor(revision = revision))

            val dispatched = (outcome as BlockingStartOutcome.Dispatched).result
            assertThat(dispatched).isInstanceOf(DispatchResult.Rejected::class.java)
            assertThat((dispatched as DispatchResult.Rejected).violations.map { it.code })
                .contains(ViolationCode.BLOCKING_ALREADY_APPLIED)
        }

    /**
     * §17 : le déclenchement est journalisé **avant toute décision**, avec son `sessionId`. C'est le
     * pendant d'`ALARM_RECEIVED` pour le réveil.
     */
    @Test
    fun theTriggerIsLoggedWithItsSessionIdBeforeAnyDecision() =
        runTest {
            val harness = deferredHarness()
            val revision = harness.currentSnapshot().revision
            harness.technicalEventLog.entries.clear()
            harness.clock.now = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS

            handlerFor(harness).handle(extrasFor(revision = revision))

            assertThat(harness.technicalEventLog.entries.first())
                .isEqualTo(TechnicalEventType.BLOCKING_START_RECEIVED to SessionDtoFixtures.SESSION_ID)
        }

    /**
     * Le cas qui motive ce type : une alarme de début ayant survécu à la fin de sa session —
     * `CANCEL_BLOCKING_START` est best-effort et peut échouer. Rien ne se produit, mais le fait doit
     * rester visible au diagnostic, sans quoi il ne laisserait aucune trace exportable.
     */
    @Test
    fun anOrphanTriggerOnAClearedSessionIsStillLogged() =
        runTest {
            val harness = TestCoordinatorHarness()

            val outcome = handlerFor(harness).handle(extrasFor(revision = 2L))

            assertThat(outcome).isEqualTo(BlockingStartOutcome.NoSession)
            assertThat(harness.technicalEventLog.entries)
                .containsExactly(TechnicalEventType.BLOCKING_START_RECEIVED to SessionDtoFixtures.SESSION_ID)
        }

    /** Extras invalides : journalisé sans `sessionId`, puisqu'aucun n'a pu être lu. */
    @Test
    fun invalidExtrasAreLoggedWithoutASessionId() =
        runTest {
            val harness = deferredHarness()
            harness.technicalEventLog.entries.clear()

            handlerFor(harness).handle(ServiceCommandExtras(sessionId = null, revision = 2L))

            assertThat(harness.technicalEventLog.entries)
                .containsExactly(TechnicalEventType.BLOCKING_START_RECEIVED to null)
        }

    /**
     * Avant l'instant de début — cas d'un déclenchement anticipé par une horloge déplacée : le moteur
     * refuse par `BLOCKING_START_NOT_REACHED` plutôt que de bloquer en avance (§5.2).
     */
    @Test
    fun aTriggerBeforeTheStartInstantIsRejectedByTheEngine() =
        runTest {
            val harness = deferredHarness()
            val revision = harness.currentSnapshot().revision
            harness.clock.now = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS - 1

            val outcome = handlerFor(harness).handle(extrasFor(revision = revision))

            val dispatched = (outcome as BlockingStartOutcome.Dispatched).result
            assertThat((dispatched as DispatchResult.Rejected).violations.map { it.code })
                .contains(ViolationCode.BLOCKING_START_NOT_REACHED)
            assertThat(harness.currentSnapshot().isBlockingPending).isTrue()
        }
}
