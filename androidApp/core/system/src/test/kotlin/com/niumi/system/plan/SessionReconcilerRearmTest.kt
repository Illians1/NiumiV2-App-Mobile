package com.niumi.system.plan

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.common.OperationResult
import com.niumi.system.plan.fakes.PlanTestHarness
import com.niumi.system.plan.fakes.WEEKDAYS_PLAN
import com.niumi.system.plan.fakes.epochMillisAt
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.ReconcileAction
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Ré-armement par la réconciliation (étape 44 ; SPEC_ANDROID §9.3) : au démarrage et au
 * déverrouillage quand aucune session n'existe et que le planning est allumé — c'est aussi le nouvel
 * essai après un échec (décision Q14 du 2026-10-24) ; après un déplacement de l'horloge ou du fuseau
 * avant le coucher (décision Q9, étendue à l'horloge le même jour).
 */
class SessionReconcilerRearmTest {
    private val fridaySeven = epochMillisAt("2026-10-23T07:00")

    @Test
    fun aProcessStartWithoutSessionArmsTheNextOccurrence() =
        runTest {
            val harness = PlanTestHarness()

            val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(result.actions).contains(ReconcileAction.PlanRearmed(fridaySeven))
            assertThat(harness.activeSnapshot()?.state).isEqualTo(SessionStateDto.ARMED)
        }

    @Test
    fun anUnlockWithoutSessionArmsTheNextOccurrence() =
        runTest {
            val harness = PlanTestHarness()

            val result = harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(result.actions).contains(ReconcileAction.PlanRearmed(fridaySeven))
        }

    @Test
    fun aProcessStartWithTheSwitchOffArmsNothing() =
        runTest {
            val harness = PlanTestHarness(plan = WEEKDAYS_PLAN.copy(enabled = false))

            val result = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(result.actions).isEmpty()
            assertThat(harness.activeSnapshot()).isNull()
        }

    /** Avant déverrouillage, le planning se lit éteint parce qu'illisible : ce n'est pas une extinction. */
    @Test
    fun aProcessStartOnALockedDeviceArmsNothingAndKeepsTheSwitch() =
        runTest {
            val harness = PlanTestHarness()
            harness.core.unlockState.isUserUnlocked = false

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.activeSnapshot()).isNull()
            assertThat(harness.planStore.current.enabled).isTrue()
        }

    /** Q14 : un échec est retenté au démarrage suivant, et l'état observable suit. */
    @Test
    fun aFailedArmingIsRetriedAtTheNextProcessStart() =
        runTest {
            val harness = PlanTestHarness()
            harness.core.alarmScheduler.scheduleResult = OperationResult.Failure("ANDROID_ALARM_SCHEDULE_FAILED")

            val failed = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)
            assertThat(failed.actions).contains(ReconcileAction.PlanRearmFailed("ANDROID_ALARM_SCHEDULE_FAILED"))
            assertThat(harness.armingStatus.state.value).isInstanceOf(PlanArmingState.Failed::class.java)

            harness.core.alarmScheduler.scheduleResult = OperationResult.Success
            val retried = harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(retried.actions).contains(ReconcileAction.PlanRearmed(fridaySeven))
            assertThat(harness.armingStatus.state.value).isEqualTo(PlanArmingState.Ok)
        }

    @Test
    fun anArmedSessionIsLeftAloneAtProcessStart() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val armed = harness.activeSnapshot()!!

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.finalStatesBySession()).containsExactly(armed.sessionId to SessionStateDto.ARMED)
        }

    @Test
    fun aNewTimeZoneBeforeBedtimeRearmsAtTheNewLocalTime() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val first = harness.activeSnapshot()!!
            harness.timeZoneProvider.zoneId = "Europe/London"

            val result = harness.coordinator.reconcile(ReconcileReason.TIMEZONE_CHANGED)

            val london = epochMillisAt("2026-10-23T07:00", zoneId = "Europe/London")
            assertThat(result.actions).contains(ReconcileAction.PlanRearmed(london))
            assertThat(harness.finalStatesBySession())
                .containsExactly(
                    first.sessionId to SessionStateDto.DISARMED,
                    harness.activeSnapshot()!!.sessionId to SessionStateDto.ARMED,
                ).inOrder()
        }

    @Test
    fun aClockResyncThatChangesNoInstantKeepsTheSession() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val armed = harness.activeSnapshot()!!

            harness.coordinator.reconcile(ReconcileReason.TIME_CHANGED)

            assertThat(harness.finalStatesBySession()).containsExactly(armed.sessionId to SessionStateDto.ARMED)
        }

    /** Après le coucher, l'instant est conservé (décision 9) : la reprogrammation au même instant suffit. */
    @Test
    fun aNewTimeZoneAfterBedtimeKeepsTheInstant() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN.copy(bedtimeIso = "19:00"), PlanChangeReason.SWITCH)
            val armed = harness.activeSnapshot()!!
            harness.timeZoneProvider.zoneId = "Europe/London"

            val result = harness.coordinator.reconcile(ReconcileReason.TIMEZONE_CHANGED)

            assertThat(
                result.actions,
            ).contains(ReconcileAction.AlarmRescheduled(armed.wakeSchedule.triggerAtEpochMillis))
            assertThat(harness.finalStatesBySession()).containsExactly(armed.sessionId to SessionStateDto.ARMED)
        }

    /** Planning éteint (session ponctuelle de l'écran 6 transitoire) : comportement d'avant l'étape 44. */
    @Test
    fun aNewTimeZoneWithTheSwitchOffKeepsTheOneOffSession() =
        runTest {
            val harness = PlanTestHarness(plan = WEEKDAYS_PLAN.copy(enabled = false))
            val oneOff =
                (
                    harness.coordinator.dispatch(
                        SessionDtoFixtures.activationRequested(
                            eventId = "00000000-0000-0000-0000-000000000001",
                            occurredAtEpochMillis = harness.core.clock.now,
                            blockingStartsAtEpochMillis = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
                        ),
                        SessionDtoFixtures.extras(),
                    ) as DispatchResult.Applied
                ).snapshot!!
            harness.timeZoneProvider.zoneId = "Europe/London"

            harness.coordinator.reconcile(ReconcileReason.TIMEZONE_CHANGED)

            assertThat(harness.finalStatesBySession()).containsExactly(oneOff.sessionId to SessionStateDto.ARMED)
        }

    /** Pointeur d'une annulation resté en place (libération sur appareil verrouillé) : rattrapée à l'ouverture. */
    @Test
    fun aCancellationStillPointedAtUnlockTurnsTheSwitchOff() =
        runTest {
            val harness = PlanTestHarness()
            harness.leaveFinishedSessionPointed(SessionStateDto.CANCELLED)

            val result = harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(result.actions).contains(ReconcileAction.PlanSwitchedOff)
            assertThat(harness.planStore.current.enabled).isFalse()
            assertThat(harness.activeSnapshot()).isNull()
        }

    @Test
    fun aCompletionStillPointedAtUnlockArmsTheNextOccurrence() =
        runTest {
            val harness = PlanTestHarness()
            harness.leaveFinishedSessionPointed(SessionStateDto.COMPLETED)

            val result = harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(result.actions).contains(ReconcileAction.PlanRearmed(fridaySeven))
            assertThat(harness.activeSnapshot()?.state).isEqualTo(SessionStateDto.ARMED)
        }
}
