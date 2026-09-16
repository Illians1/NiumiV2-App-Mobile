package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.isBlockingPending
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

    /**
     * Activation à blocage **différé** (Lot 6) : l'alarme de début est programmée à l'instant
     * contractuel et **aucune application n'est bloquée** — la session est pourtant pleinement armée.
     * C'est la différence qui fonde tout le lot : `ARMED` ne signifie plus « applications bloquées ».
     */
    @Test
    fun aDeferredActivationSchedulesTheBlockingStartWithoutBlockingAnything() =
        runTest {
            val harness = TestCoordinatorHarness()

            val result =
                harness.coordinator.dispatch(
                    SessionDtoFixtures.activationRequested(
                        eventId = eventId,
                        blockingStartsAtEpochMillis = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
                    ),
                    SessionDtoFixtures.extras(),
                )

            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isTrue()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(applied.snapshot?.isBlockingPending).isTrue()
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isTrue()
            assertThat(harness.blockingStartScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isTrue()
            assertThat(harness.blockingStartScheduler.lastScheduledAtEpochMillis)
                .isEqualTo(SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS)
            assertThat(harness.blockingController.effectivePackages()).isEmpty()
            assertThat(harness.journal.calls).doesNotContain("BlockingController.apply")
        }

    /**
     * SPEC_ANDROID §18 : « un échec de `SCHEDULE_BLOCKING_START` pendant `PREPARING` fait échouer
     * l'activation comme un échec de `SCHEDULE_ALARM` ». Le rollback annule les deux alarmes et
     * efface le pointeur : une session armée dont le blocage ne commencerait jamais serait un
     * engagement que Niumi ne tiendrait pas.
     */
    @Test
    fun aFailedBlockingStartSchedulingRollsBackAndFailsActivation() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.blockingStartScheduler.scheduleResult = OperationResult.Failure("ANDROID_EXACT_ALARM_DENIED")

            val result =
                harness.coordinator.dispatch(
                    SessionDtoFixtures.activationRequested(
                        eventId = eventId,
                        blockingStartsAtEpochMillis = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
                    ),
                    SessionDtoFixtures.extras(),
                )

            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isFalse()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.FAILED)
            assertThat(applied.snapshot?.failureCode).isEqualTo("ANDROID_EXACT_ALARM_DENIED")
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isFalse()
            assertThat(harness.blockingStartScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isFalse()
            assertThat(harness.journal.calls).contains("BlockingStartScheduler.cancel")
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
        }

    /**
     * Non-régression du contrat 1.2 : une activation **immédiate** ne produit aucun effet de début
     * différé. Ses `effectId` et l'ordre de ses effets restent donc exactement ceux d'avant le Lot 6
     * — c'est ce qui rend la non-régression démontrable plutôt que supposée (`ETAPE-22.md`).
     */
    @Test
    fun anImmediateActivationProducesNoBlockingStartEffectAtAll() =
        runTest {
            val harness = TestCoordinatorHarness()

            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(eventId = eventId),
                SessionDtoFixtures.extras(),
            )

            assertThat(harness.blockingStartScheduler.scheduleCount).isEqualTo(0)
            assertThat(harness.journal.calls).doesNotContain("BlockingStartScheduler.schedule")
            assertThat(harness.journal.calls).doesNotContain("BlockingStartScheduler.cancel")
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
