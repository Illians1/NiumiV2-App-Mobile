package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.ViolationCode
import com.niumi.core.interop.ReleaseMeansDto
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Désarmement d'une session en attente (SPEC_CORE_KMP §5.1, §6, §12 ; étape 44). Une seule
 * dispatch de `DISARM_REQUESTED` mène à `DISARMED` : le coordinateur referme la phase par
 * `RELEASE_SUCCEEDED` dès que l'alarme est annulée, seul effet requis du désarmement.
 */
class SessionCoordinatorDisarmTest {
    private suspend fun armDeferredSession(harness: TestCoordinatorHarness): SessionSnapshotDto {
        val result =
            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(
                    eventId = "00000000-0000-0000-0000-000000000001",
                    blockingStartsAtEpochMillis = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
                ),
                SessionDtoFixtures.extras(),
            )
        return (result as DispatchResult.Applied).snapshot!!
    }

    @Test
    fun aPendingSessionIsDisarmedInASingleDispatch() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armDeferredSession(harness)

            val result = harness.coordinator.dispatch(harness.eventFactory.disarmRequested(armed))

            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isTrue()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.DISARMED)
            assertThat(applied.snapshot?.releaseTarget).isEqualTo(ReleaseTargetDto.DISARMED)
            assertThat(applied.snapshot?.releasedBy).isEqualTo(ReleaseMeansDto.PLAN_CHANGE)
            assertThat(applied.snapshot?.disarmedAtEpochMillis).isNotNull()
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isFalse()
            assertThat(harness.blockingStartScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isFalse()
            assertThat(harness.journal.calls).doesNotContain("BlockingController.apply")
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
        }

    @Test
    fun theDisarmedStateIsLoggedAsSessionDisarmed() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armDeferredSession(harness)

            harness.coordinator.dispatch(harness.eventFactory.disarmRequested(armed))

            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.SESSION_DISARMED)
        }

    /** §18 : une annulation d'alarme qui échoue garde `RELEASING` ; jamais un `DISARMED` qui sonnerait. */
    @Test
    fun aFailedAlarmCancellationKeepsReleasingAndRecordsAPartialFailure() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armDeferredSession(harness)
            harness.alarmScheduler.cancelResult = OperationResult.Failure("ANDROID_CANCEL_FAILED")

            val result = harness.coordinator.dispatch(harness.eventFactory.disarmRequested(armed))

            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isFalse()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.RELEASING)
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.RELEASE_PARTIAL_FAILURE)
        }

    /** Un échec de `CANCEL_BLOCKING_START` reste best-effort (SPEC_CORE_KMP §6). */
    @Test
    fun aFailedBlockingStartCancellationStillReachesDisarmed() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armDeferredSession(harness)
            harness.blockingStartScheduler.cancelResult = OperationResult.Failure("ANDROID_CANCEL_FAILED")

            val result = harness.coordinator.dispatch(harness.eventFactory.disarmRequested(armed))

            assertThat((result as DispatchResult.Applied).snapshot?.state).isEqualTo(SessionStateDto.DISARMED)
        }

    @Test
    fun anImmediateBlockingSessionCannotBeDisarmed() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed =
                (
                    harness.coordinator.dispatch(
                        SessionDtoFixtures.activationRequested(eventId = "00000000-0000-0000-0000-000000000001"),
                        SessionDtoFixtures.extras(),
                    ) as DispatchResult.Applied
                ).snapshot!!

            val result = harness.coordinator.dispatch(harness.eventFactory.disarmRequested(armed))

            assertThat((result as DispatchResult.Rejected).violations.map { it.code })
                .containsExactly(ViolationCode.BLOCKING_ALREADY_APPLIED)
        }
}
