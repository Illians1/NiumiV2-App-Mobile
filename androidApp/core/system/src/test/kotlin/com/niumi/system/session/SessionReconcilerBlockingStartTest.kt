package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.isBlockingPending
import com.niumi.core.schedule.TriggerDelayPolicy
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Réconciliation du début d'un blocage différé (SPEC_ANDROID §12.4, §9.3 ; SPEC_CORE_KMP §8.3).
 * C'est le second chemin normal du Lot 6, après `BlockingStartReceiver` : au premier réveil du
 * processus, une session dont l'instant de début est atteint doit voir son blocage demandé, et une
 * alarme disparue être reposée au **même** instant.
 */
class SessionReconcilerBlockingStartTest {
    private val extras = SessionDtoFixtures.extras()
    private val startsAt = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS

    private suspend fun seed(
        harness: TestCoordinatorHarness,
        snapshot: SessionSnapshotDto,
    ) {
        harness.gateway.commit(
            StoredDecision(
                snapshot = snapshot,
                receipt = EventReceipt("seed", snapshot.sessionId, "hash", snapshot.revision, 900L),
                effects = emptyList(),
                androidExtras = extras,
            ),
        )
    }

    /** Session `ARMED` différée, semée directement : l'activation elle-même est testée ailleurs. */
    private suspend fun armedDeferred(
        harness: TestCoordinatorHarness,
        blockingAppliedAtEpochMillis: Long? = null,
    ): SessionSnapshotDto {
        val snapshot =
            SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = blockingAppliedAtEpochMillis)
        seed(harness, snapshot)
        return snapshot
    }

    private suspend fun TestCoordinatorHarness.currentSnapshot() = (gateway.load() as LoadResult.Present).snapshot

    /** L'instant n'est pas atteint et l'alarme est là : la passe ne touche à rien. */
    @Test
    fun aPendingBlockingWhoseAlarmIsStillScheduledIsLeftAlone() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot = armedDeferred(harness)
            harness.blockingStartScheduler.schedule(snapshot.sessionId, snapshot.revision, startsAt)
            harness.clock.now = startsAt - 60_000L

            val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(result.actions.filterIsInstance<ReconcileAction.BlockingStartRescheduled>()).isEmpty()
            assertThat(harness.currentSnapshot().isBlockingPending).isTrue()
        }

    /**
     * §12.4 : « instant de début non atteint et alarme de début absente → reprogrammer au même
     * instant ». Le cas type est le redémarrage de l'appareil, qui efface toutes les alarmes.
     */
    @Test
    fun aPendingBlockingWhoseAlarmDisappearedIsRescheduledAtTheSameInstant() =
        runTest {
            val harness = TestCoordinatorHarness()
            armedDeferred(harness)
            harness.clock.now = startsAt - 60_000L

            val result = harness.coordinator.reconcile(ReconcileReason.BOOT)

            assertThat(result.actions).contains(ReconcileAction.BlockingStartRescheduled(startsAt))
            assertThat(harness.blockingStartScheduler.lastScheduledAtEpochMillis).isEqualTo(startsAt)
            assertThat(harness.technicalEventLog.entries)
                .contains(TechnicalEventType.BLOCKING_START_RESCHEDULED to SessionDtoFixtures.SESSION_ID)
            assertThat(harness.currentSnapshot().isBlockingPending).isTrue()
        }

    /**
     * §9.3 : sur un déplacement d'horloge, le réenregistrement est **inconditionnel** — un
     * `PendingIntent` encore présent ne prouve pas que le système l'a conservé au bon instant. Et
     * l'instant reposé reste le même : `startsAtEpochMillis` est immuable après l'activation (§8.3).
     */
    @Test
    fun aClockChangeReschedulesTheBlockingStartUnconditionallyAtTheSameInstant() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot = armedDeferred(harness)
            harness.blockingStartScheduler.schedule(snapshot.sessionId, snapshot.revision, startsAt)
            harness.clock.now = startsAt - 60_000L

            val result = harness.coordinator.reconcile(ReconcileReason.TIME_CHANGED)

            assertThat(result.actions).contains(ReconcileAction.BlockingStartRescheduled(startsAt))
            assertThat(harness.blockingStartScheduler.lastScheduledAtEpochMillis).isEqualTo(startsAt)
        }

    /**
     * Instant atteint depuis moins de quinze minutes : le blocage est demandé sans incident. Contraire
     * au réveil, aucune alarme immédiate n'est reposée — « un blocage ne se manque pas, il s'applique
     * en retard » (§8.3) — et ce quelle que soit la raison de la passe, pas seulement `BEFORE_SCAN`.
     */
    @Test
    fun aReachedBlockingStartAppliesTheBlockingWithoutAnyIncident() =
        runTest {
            val harness = TestCoordinatorHarness()
            armedDeferred(harness)
            harness.clock.now = startsAt + 60_000L

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            val snapshot = harness.currentSnapshot()
            assertThat(snapshot.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(snapshot.isBlockingPending).isFalse()
            assertThat(snapshot.blockingAppliedAtEpochMillis).isEqualTo(startsAt + 60_000L)
            assertThat(snapshot.health).isEqualTo(SessionHealthDto.HEALTHY)
            assertThat(harness.journal.calls).contains("BlockingController.apply")
            assertThat(harness.technicalEventLog.entries.map { it.first })
                .doesNotContain(TechnicalEventType.MISSED_BLOCKING_START_WINDOW)
        }

    /**
     * Au-delà de quinze minutes, `MISSED_BLOCKING_START_WINDOW` est consigné en `WARNING` : le blocage
     * s'applique quand même, et la santé **ne se dégrade pas** — contrairement au réveil manqué
     * (`MISSED_TRIGGER_WINDOW`, `DEGRADED`). Un blocage en retard n'a rompu aucune promesse.
     */
    @Test
    fun aBlockingStartMissedByMoreThanFifteenMinutesStillAppliesAndWarnsWithoutDegrading() =
        runTest {
            val harness = TestCoordinatorHarness()
            armedDeferred(harness)
            harness.clock.now = startsAt + TriggerDelayPolicy.GRACE_WINDOW_MILLIS + 1

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            val snapshot = harness.currentSnapshot()
            assertThat(snapshot.isBlockingPending).isFalse()
            assertThat(snapshot.health).isEqualTo(SessionHealthDto.HEALTHY)
            assertThat(harness.technicalEventLog.entries)
                .contains(TechnicalEventType.MISSED_BLOCKING_START_WINDOW to SessionDtoFixtures.SESSION_ID)
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.MISSED_BLOCKING_START_WINDOW)
        }

    /**
     * §12.4 : « la garde de permission de `reconcileArmed` s'applique avant, comme pour le réveil ».
     * Sans service d'accessibilité, appliquer le blocage n'a pas de sens et l'incident est déjà
     * consigné par la surveillance de §13.1.
     */
    @Test
    fun aLostAccessibilityPermissionStopsThePassBeforeTheBlockingStart() =
        runTest {
            val harness = TestCoordinatorHarness()
            armedDeferred(harness)
            harness.clock.now = startsAt + 60_000L
            harness.accessibilityServiceStatus.enabled = false

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.currentSnapshot().isBlockingPending).isTrue()
            assertThat(harness.journal.calls).doesNotContain("BlockingController.apply")
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.BLOCKING_PERMISSION_REVOKED)
        }

    /** Blocage déjà demandé : plus rien à faire, et surtout aucune seconde application. */
    @Test
    fun anAlreadyAppliedBlockingIsLeftAlone() =
        runTest {
            val harness = TestCoordinatorHarness()
            armedDeferred(harness, blockingAppliedAtEpochMillis = startsAt)
            harness.clock.now = startsAt + 60_000L

            val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(result.actions.filterIsInstance<ReconcileAction.BlockingStartRescheduled>()).isEmpty()
            assertThat(harness.journal.calls).doesNotContain("BlockingController.apply")
            assertThat(harness.blockingStartScheduler.scheduleCount).isEqualTo(0)
        }

    /** Une session à blocage immédiat n'a pas d'alarme de début : la passe ne doit rien y reprogrammer. */
    @Test
    fun anImmediateSessionNeverSchedulesABlockingStart() =
        runTest {
            val harness = TestCoordinatorHarness()
            seed(
                harness,
                SessionDtoFixtures.deferredArmedSnapshot(
                    startsAtEpochMillis = null,
                    blockingAppliedAtEpochMillis = 1_000L,
                ),
            )
            harness.clock.now = startsAt + 60_000L

            val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(result.actions.filterIsInstance<ReconcileAction.BlockingStartRescheduled>()).isEmpty()
            assertThat(harness.blockingStartScheduler.scheduleCount).isEqualTo(0)
        }

    /**
     * Les deux instants dépassés dans la même passe. Le début du blocage est traité **avant** le
     * retard du réveil (§12.4), et le `TRIGGER_ELAPSED` qui suit part de la révision rendue par le
     * `BLOCKING_START_ELAPSED` — sans la relecture, il tomberait en `STALE_REVISION` et le scan
     * deviendrait impossible, exactement le défaut mesuré sur appareil à l'étape 18.
     */
    @Test
    fun bothInstantsElapsedApplyTheBlockingFirstThenTheTriggerWithoutStaleRevision() =
        runTest {
            val harness = TestCoordinatorHarness()
            armedDeferred(harness)
            harness.clock.now = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + 60_000L

            harness.coordinator.reconcile(ReconcileReason.BEFORE_SCAN)

            val snapshot = harness.currentSnapshot()
            assertThat(snapshot.isBlockingPending).isFalse()
            assertThat(snapshot.state).isEqualTo(SessionStateDto.TRIGGERED_AWAITING_NFC)
            val applyIndex = harness.journal.calls.indexOf("BlockingController.apply")
            val scanIndex = harness.journal.calls.indexOfFirst { it.startsWith("ScanRequestNotifier.present") }
            assertThat(applyIndex).isAtLeast(0)
            assertThat(scanIndex).isAtLeast(0)
            assertThat(applyIndex).isLessThan(scanIndex)
        }
}
