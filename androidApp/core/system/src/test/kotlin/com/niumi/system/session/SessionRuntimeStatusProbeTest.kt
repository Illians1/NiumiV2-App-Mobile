package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * [SessionRuntimeReconciler] (SPEC_ANDROID §7.1, §18 ; étape 20), exercé directement plutôt qu'à
 * travers le réconciliateur complet : `reconcileArmed`/`reconcileTriggerDelay` reprogramment déjà
 * l'alarme dans leur propre chemin sur `ARMED`, ce qui masquerait la réparation propre à cette
 * classe. Le nom du fichier est celui du plan ; le contenu prouve le comportement de la classe.
 */
class SessionRuntimeStatusProbeTest {
    private suspend fun armSession(harness: TestCoordinatorHarness) =
        (
            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(eventId = "00000000-0000-0000-0000-000000000001"),
                SessionDtoFixtures.extras(),
            ) as DispatchResult.Applied
        ).snapshot!!

    @Test
    fun aScheduledAlarmThatVanishedIsReprogrammedBeforeAnyIncident() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)
            harness.alarmScheduler.cancel(armed.sessionId)
            assertThat(harness.alarmScheduler.isScheduled(armed.sessionId)).isFalse()

            harness.runtimeReconciler.reconcile(armed) { event -> harness.coordinator.dispatch(event) }

            assertThat(harness.alarmScheduler.isScheduled(armed.sessionId)).isTrue()
            assertThat(harness.gateway.incidentsRecorded).isEmpty()
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.ALARM_RESCHEDULED)
        }

    @Test
    fun anAlarmGapThatSurvivesTheRepairProducesAlarmPermissionRevokedOnce() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)
            harness.alarmScheduler.cancel(armed.sessionId)
            harness.alarmScheduler.scheduleResult = OperationResult.Failure("ANDROID_EXACT_ALARM_DENIED")

            harness.runtimeReconciler.reconcile(armed) { event -> harness.coordinator.dispatch(event) }
            val afterFirstPass = (harness.gateway.load() as LoadResult.Present).snapshot
            harness.runtimeReconciler.reconcile(afterFirstPass) { event -> harness.coordinator.dispatch(event) }

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .containsExactly(IncidentCodes.ALARM_PERMISSION_REVOKED)
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.EXACT_ALARM_LOST)
        }

    @Test
    fun nfcDisabledDuringRingingProducesNfcDisabledOnceWithoutChangingTheState() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)
            val ringing =
                (
                    harness.coordinator.dispatch(harness.eventFactory.alarmFired(armed)) as DispatchResult.Applied
                ).snapshot!!
            harness.runtimeStatusProbe.nfcReady = false

            harness.runtimeReconciler.reconcile(ringing) { event -> harness.coordinator.dispatch(event) }
            val afterFirstPass = (harness.gateway.load() as LoadResult.Present).snapshot
            harness.runtimeReconciler.reconcile(afterFirstPass) { event -> harness.coordinator.dispatch(event) }

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .containsExactly(IncidentCodes.NFC_DISABLED)
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.NFC_DISABLED)
            assertThat(afterFirstPass.state).isEqualTo(SessionStateDto.RINGING)
            assertThat(harness.blockingController.effectivePackages()).isNotEmpty()
        }

    /**
     * `SessionReadinessMonitor` (§13.1, `EXACT_ALARM`) couvre déjà une permission d'alarme exacte
     * absente, et `reconcileArmed` interrompt la passe avant toute reprogrammation dans ce cas
     * précis (`armedWithExactAlarmPermissionRevokedNeverReschedulesTheAlarm`). Cette classe ne
     * doit pas retenter un `schedule()` voué au même échec, ni produire de second incident.
     */
    @Test
    fun anAlarmGapIsIgnoredWhenTheExactAlarmPermissionItselfIsMissing() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)
            harness.alarmScheduler.cancel(armed.sessionId)
            harness.alarmScheduler.canScheduleExactValue = false

            harness.runtimeReconciler.reconcile(armed) { event -> harness.coordinator.dispatch(event) }

            assertThat(harness.alarmScheduler.isScheduled(armed.sessionId)).isFalse()
            assertThat(harness.gateway.incidentsRecorded).isEmpty()
            assertThat(harness.technicalEventLog.logged).doesNotContain(TechnicalEventType.ALARM_RESCHEDULED)
        }

    @Test
    fun aHealthyRuntimeProducesNothing() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)

            harness.runtimeReconciler.reconcile(armed) { event -> harness.coordinator.dispatch(event) }

            assertThat(harness.gateway.incidentsRecorded).isEmpty()
        }
}
