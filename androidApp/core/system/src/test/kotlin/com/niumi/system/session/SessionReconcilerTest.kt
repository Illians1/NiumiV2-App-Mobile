package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.core.schedule.TriggerDelayPolicy
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.blocking.BlockedPackagesState
import com.niumi.system.readiness.AndroidIncidentCodes
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * `SessionReconciler` : reprise d'un état incomplet, comparaison de l'état métier aux
 * sous-systèmes Android (SPEC_CORE_KMP §6.1, §13 ; SPEC_ANDROID §9.2, §9.3, §13.1, §18). La
 * politique de retard est toujours lue via `NiumiCoreFacade.evaluateTriggerDelay()`.
 */
class SessionReconcilerTest {
    private val extras = SessionDtoFixtures.extras()

    private fun preparingSnapshot(revision: Long = 1) =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = revision,
            sessionId = SessionDtoFixtures.SESSION_ID,
            wakeSchedule =
                WakeScheduleDto("2026-09-10", "07:00", "Europe/Paris", SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS),
            state = SessionStateDto.PREPARING,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1_000L,
            armedAtEpochMillis = null,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    private fun armedSnapshot(revision: Long = 2) =
        preparingSnapshot(revision).copy(state = SessionStateDto.ARMED, armedAtEpochMillis = 1_000L)

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

    @Test
    fun preparingWithAlarmScheduledAndBlockingActiveResumesToArmed() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot = preparingSnapshot()
            seed(harness, snapshot)
            harness.alarmScheduler.schedule(
                snapshot.sessionId,
                snapshot.revision,
                snapshot.wakeSchedule.triggerAtEpochMillis,
            )
            harness.blockedPackagesProjection.state =
                BlockedPackagesState.Active(snapshot.sessionId, extras.blockedPackages.toSet())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            val loaded = harness.gateway.load() as LoadResult.Present
            assertThat(loaded.snapshot.state).isEqualTo(SessionStateDto.ARMED)
        }

    @Test
    fun preparingWithoutAlarmScheduledRollsBackToFailed() =
        runTest {
            val harness = TestCoordinatorHarness()
            seed(harness, preparingSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            // Le pointeur actif est effacé par CLEAR_ACTIVE_SESSION, effet de ACTIVATION_FAILED.
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
            assertThat(
                harness.publisher.snapshot.value
                    ?.state,
            ).isEqualTo(SessionStateDto.FAILED)
            assertThat(
                harness.publisher.snapshot.value
                    ?.failureCode,
            ).isEqualTo("ANDROID_ACTIVATION_INTERRUPTED")
        }

    @Test
    fun armedWithAlarmNotScheduledAndTriggerNotReachedReschedulesAtTheSameTriggerAt() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = 1_000L
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isTrue()
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.ALARM_RESCHEDULED)
            val loaded = harness.gateway.load() as LoadResult.Present
            assertThat(loaded.snapshot.state).isEqualTo(SessionStateDto.ARMED)
        }

    @Test
    fun armedPastTheGraceWindowProducesTriggerElapsedWithMissedTriggerWindowIncident() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + TriggerDelayPolicy.GRACE_WINDOW_MILLIS + 1
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            val loaded = harness.gateway.load() as LoadResult.Present
            assertThat(loaded.snapshot.state).isEqualTo(SessionStateDto.TRIGGERED_AWAITING_NFC)
            assertThat(
                harness.gateway.incidentsRecorded.map { it.second.code },
            ).contains(IncidentCodes.MISSED_TRIGGER_WINDOW)
        }

    @Test
    fun armedWithinGraceWindowAndReasonOtherThanBeforeScanReschedulesImmediately() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + 60_000L
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isTrue()
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.ALARM_RESCHEDULED)
            val loaded = harness.gateway.load() as LoadResult.Present
            assertThat(loaded.snapshot.state).isEqualTo(SessionStateDto.ARMED)
        }

    @Test
    fun armedWithinGraceWindowAndBeforeScanReasonProducesTriggerElapsedWithoutIncident() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + 60_000L
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.BEFORE_SCAN)

            val loaded = harness.gateway.load() as LoadResult.Present
            assertThat(loaded.snapshot.state).isEqualTo(SessionStateDto.TRIGGERED_AWAITING_NFC)
            assertThat(harness.gateway.incidentsRecorded).isEmpty()
        }

    @Test
    fun armedWithAccessibilityServiceDisabledReportsAnIncidentAndKeepsTheStateArmed() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = 1_000L
            harness.accessibilityServiceStatus.enabled = false
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            val loaded = harness.gateway.load() as LoadResult.Present
            assertThat(loaded.snapshot.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.BLOCKING_PERMISSION_REVOKED)
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.ACCESSIBILITY_DISABLED)
        }

    @Test
    fun armedWithADegradedAndroidControlStillEvaluatesTheTriggerDelay() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = 1_000L
            // Volume d'alarme à zéro : le réveil devient inaudible, mais il reste programmé —
            // l'incident ne doit pas interrompre la passe (SPEC_ANDROID §13.1).
            harness.readinessSources.alarmVolumeSource.volume = 0
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(AndroidIncidentCodes.ALARM_VOLUME_ZERO)
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isTrue()
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.ALARM_RESCHEDULED)
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.SESSION_READINESS_DEGRADED)
            assertThat(harness.warningNotifier.presented).hasSize(1)
        }

    @Test
    fun armedWithExactAlarmPermissionRevokedNeverReschedulesTheAlarm() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = 1_000L
            harness.alarmScheduler.canScheduleExactValue = false
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            // Reprogrammer une alarme exacte sans y avoir droit n'aurait aucun sens : la passe
            // s'arrête après l'incident, comme à l'étape 11.
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isFalse()
            assertThat(harness.technicalEventLog.logged).doesNotContain(TechnicalEventType.ALARM_RESCHEDULED)
        }

    @Test
    fun aDegradedControlIsReportedOnceAcrossSuccessiveReconciliations() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = 1_000L
            harness.readinessSources.alarmVolumeSource.volume = 0
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)
            harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(
                harness.gateway.incidentsRecorded.count { it.second.code == AndroidIncidentCodes.ALARM_VOLUME_ZERO },
            ).isEqualTo(1)
            assertThat(harness.warningNotifier.presented).hasSize(1)
        }

    @Test
    fun armedWithExactAlarmPermissionRevokedReportsAnIncidentAndKeepsTheStateArmed() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.clock.now = 1_000L
            harness.alarmScheduler.canScheduleExactValue = false
            seed(harness, armedSnapshot())

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            val loaded = harness.gateway.load() as LoadResult.Present
            assertThat(loaded.snapshot.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(
                harness.gateway.incidentsRecorded.map { it.second.code },
            ).contains(IncidentCodes.ALARM_PERMISSION_REVOKED)
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.EXACT_ALARM_LOST)
        }
}
