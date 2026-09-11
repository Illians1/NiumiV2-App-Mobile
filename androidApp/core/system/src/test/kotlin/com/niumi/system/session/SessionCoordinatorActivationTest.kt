package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Activation en deux phases (SPEC_CORE_KMP §6, §10 ; SPEC_ANDROID §9.2). */
class SessionCoordinatorActivationTest {
    private val eventId = "00000000-0000-0000-0000-000000000001"

    @Test
    fun activationRequestedPersistsBeforeAnyEffectThenSucceedsAndArms() =
        runTest {
            val harness = TestCoordinatorHarness()

            val result =
                harness.coordinator.dispatch(
                    SessionDtoFixtures.activationRequested(eventId = eventId),
                    SessionDtoFixtures.extras(),
                )

            // Ordre : la persistance (premier `commit`) précède tout appel aux fakes système.
            val commitIndex = harness.journal.calls.indexOf("gateway.commit")
            val scheduleIndex = harness.journal.calls.indexOf("AlarmScheduler.schedule")
            val applyIndex = harness.journal.calls.indexOf("BlockingController.apply")
            assertThat(commitIndex).isEqualTo(0)
            assertThat(commitIndex).isLessThan(scheduleIndex)
            assertThat(commitIndex).isLessThan(applyIndex)

            assertThat(result).isInstanceOf(DispatchResult.Applied::class.java)
            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isTrue()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isTrue()
            assertThat(harness.blockingController.effectivePackages()).isNotEmpty()
        }

    @Test
    fun scheduleAlarmFailureRollsBackAndFailsActivation() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.alarmScheduler.scheduleResult = OperationResult.Failure("ANDROID_ALARM_SCHEDULE_FAILED")

            val result =
                harness.coordinator.dispatch(
                    SessionDtoFixtures.activationRequested(eventId = eventId),
                    SessionDtoFixtures.extras(),
                )

            assertThat(result).isInstanceOf(DispatchResult.Applied::class.java)
            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isFalse()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.FAILED)
            assertThat(applied.snapshot?.failureCode).isEqualTo("ANDROID_ALARM_SCHEDULE_FAILED")
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isFalse()
            assertThat(harness.blockingController.effectivePackages()).isEmpty()
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
        }

    @Test
    fun publishSnapshotFailureIsBestEffortAndDoesNotPreventActivationSucceeded() =
        runTest {
            val harness = TestCoordinatorHarness()
            val alwaysFailing =
                EffectExecutor { _, _, _ -> ExecutionOutcome(OperationResult.Failure("ANDROID_PUBLISH_FAILED")) }
            val coordinator =
                harness.coordinatorWithExecutorOverride(SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT, alwaysFailing)

            val result =
                coordinator.dispatch(
                    SessionDtoFixtures.activationRequested(eventId = eventId),
                    SessionDtoFixtures.extras(),
                )

            assertThat(result).isInstanceOf(DispatchResult.Applied::class.java)
            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isTrue()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.ARMED)
        }
}
