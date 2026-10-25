package com.niumi.system.plan

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.BlockedPackage
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.plan.fakes.PlanTestHarness
import com.niumi.system.plan.fakes.WEEKDAYS_PLAN
import com.niumi.system.plan.fakes.epochMillisAt
import com.niumi.system.session.LoadResult
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Le planning arme, désarme et ré-arme sous le verrou du coordinateur (étape 44 ; SPEC_CORE_KMP §2
 * décision 4, §8.4 ; SPEC_ANDROID §9.2). Jeudi 22 octobre 2026 à 20:00 à Paris, planning lundi à
 * vendredi 7:00, coucher 22:30 : la prochaine occurrence est vendredi 7:00, coucher jeudi 22:30.
 */
class WakePlanSchedulerTest {
    private val fridaySeven = epochMillisAt("2026-10-23T07:00")
    private val thursdayBedtime = epochMillisAt("2026-10-22T22:30")

    @Test
    fun switchingOnArmsTheNextOccurrence() =
        runTest {
            val harness = PlanTestHarness(plan = WEEKDAYS_PLAN.copy(enabled = false))

            val result = harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)

            assertThat(result).isInstanceOf(PlanApplyResult.Armed::class.java)
            val armed = harness.activeSnapshot()!!
            assertThat(armed.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(armed.wakeSchedule.triggerAtEpochMillis).isEqualTo(fridaySeven)
            assertThat(armed.blockingSchedule.startsAtEpochMillis).isEqualTo(thursdayBedtime)
            assertThat(harness.planStore.current).isEqualTo(WEEKDAYS_PLAN)
            assertThat(harness.core.technicalEventLog.logged).contains(TechnicalEventType.PLAN_REARMED)
        }

    /** Critère de fin de l'étape 44 : une session `DISARMED` et une `ARMED`, deux `sessionId`. */
    @Test
    fun changingTheWakeTimeBeforeBedtimeDisarmsThenArmsANewSession() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val first = harness.activeSnapshot()!!

            val result = harness.scheduler.apply(WEEKDAYS_PLAN.copy(wakeTimeIso = "06:30"), PlanChangeReason.WAKE_TIME)

            assertThat(result).isInstanceOf(PlanApplyResult.Armed::class.java)
            val second = harness.activeSnapshot()!!
            assertThat(second.sessionId).isNotEqualTo(first.sessionId)
            assertThat(second.wakeSchedule.triggerAtEpochMillis).isEqualTo(epochMillisAt("2026-10-23T06:30"))
            assertThat(harness.finalStatesBySession())
                .containsExactly(first.sessionId to SessionStateDto.DISARMED, second.sessionId to SessionStateDto.ARMED)
                .inOrder()
            assertThat(harness.core.alarmScheduler.isScheduled(first.sessionId)).isFalse()
            assertThat(harness.core.alarmScheduler.isScheduled(second.sessionId)).isTrue()
            assertThat(harness.planStore.current.wakeTimeIso).isEqualTo("06:30")
        }

    /** Décision 11 amendée le 2026-10-24 : la sélection se modifie avant le coucher, comme les heures. */
    @Test
    fun aNewAppSelectionBeforeBedtimeIsArmedInANewSession() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val first = harness.activeSnapshot()!!
            val newSelection =
                listOf(BlockedPackage("com.example.video", "Vidéo"), BlockedPackage("com.example.game", "Jeu"))
            harness.appSelectionStore.stored = newSelection

            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.APP_SELECTION)

            val present = harness.core.gateway.load() as LoadResult.Present
            assertThat(present.snapshot.sessionId).isNotEqualTo(first.sessionId)
            assertThat(present.extras.blockedPackages).isEqualTo(newSelection)
        }

    /** Du coucher au scan, rien ne se modifie, et le planning reçu n'est pas enregistré. */
    @Test
    fun afterTheBlockingHasStartedThePlanIsLockedBySession() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN.copy(bedtimeIso = "19:00"), PlanChangeReason.BEDTIME)
            val armed = harness.activeSnapshot()!!
            assertThat(armed.blockingAppliedAtEpochMillis).isNotNull()

            val result = harness.scheduler.apply(WEEKDAYS_PLAN.copy(wakeTimeIso = "08:00"), PlanChangeReason.WAKE_TIME)

            assertThat(result).isEqualTo(PlanApplyResult.LockedBySession)
            assertThat(harness.activeSnapshot()?.sessionId).isEqualTo(armed.sessionId)
            assertThat(harness.planStore.current.wakeTimeIso).isEqualTo("07:00")
        }

    /**
     * Coucher atteint mais `BLOCKING_START_ELAPSED` pas encore traité (alarme de début en retard) :
     * le moteur refuse le désarmement (`BLOCKING_START_ALREADY_ELAPSED`). Le blocage dû est appliqué,
     * et la session reste verrouillée.
     */
    @Test
    fun aBedtimeReachedButNotYetHandledAppliesTheBlockingAndLocks() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val armed = harness.activeSnapshot()!!
            harness.core.clock.now = epochMillisAt("2026-10-22T22:40")

            val result = harness.scheduler.apply(WEEKDAYS_PLAN.copy(wakeTimeIso = "08:00"), PlanChangeReason.WAKE_TIME)

            assertThat(result).isEqualTo(PlanApplyResult.LockedBySession)
            val current = harness.activeSnapshot()!!
            assertThat(current.sessionId).isEqualTo(armed.sessionId)
            assertThat(current.blockingAppliedAtEpochMillis).isNotNull()
            assertThat(harness.planStore.current.wakeTimeIso).isEqualTo("07:00")
        }

    /** Déjà dans la période : le blocage commence tout de suite, sans confirmation (§2). */
    @Test
    fun aBedtimeAlreadyPassedBlocksImmediately() =
        runTest {
            val harness = PlanTestHarness(plan = WEEKDAYS_PLAN.copy(enabled = false))

            harness.scheduler.apply(WEEKDAYS_PLAN.copy(bedtimeIso = "19:00"), PlanChangeReason.SWITCH)

            assertThat(harness.activeSnapshot()?.blockingAppliedAtEpochMillis).isNotNull()
        }

    /**
     * « Ponctuel » (décision de Mehdi du 2026-10-25, qui règle D17) : aucun jour coché arme la
     * prochaine heure de réveil, quel que soit le jour.
     */
    @Test
    fun noDayCheckedArmsAOneOffAlarmAtTheNextWakeTime() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val weekly = harness.activeSnapshot()!!

            val result = harness.scheduler.apply(WEEKDAYS_PLAN.copy(enabledIsoDays = emptySet()), PlanChangeReason.DAYS)

            assertThat(result).isInstanceOf(PlanApplyResult.Armed::class.java)
            val oneOff = harness.activeSnapshot()!!
            assertThat(oneOff.sessionId).isNotEqualTo(weekly.sessionId)
            assertThat(oneOff.wakeSchedule.triggerAtEpochMillis).isEqualTo(fridaySeven)
            assertThat(harness.planStore.current.enabled).isTrue()
        }

    /** Décision du 2026-10-25 : deux heures identiques bloquent 24 h, jusqu'au réveil. */
    @Test
    fun aBedtimeEqualToTheWakeTimeArmsATwentyFourHourPeriod() =
        runTest {
            val harness = PlanTestHarness(plan = WEEKDAYS_PLAN.copy(enabled = false))

            val result = harness.scheduler.apply(WEEKDAYS_PLAN.copy(bedtimeIso = "07:00"), PlanChangeReason.SWITCH)

            assertThat(result).isInstanceOf(PlanApplyResult.Armed::class.java)
            val armed = harness.activeSnapshot()!!
            assertThat(armed.wakeSchedule.triggerAtEpochMillis).isEqualTo(fridaySeven)
            assertThat(armed.blockingAppliedAtEpochMillis).isNotNull()
        }

    @Test
    fun switchingOffDisarmsAndArmsNothing() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val armed = harness.activeSnapshot()!!

            val result = harness.scheduler.apply(WEEKDAYS_PLAN.copy(enabled = false), PlanChangeReason.SWITCH)

            assertThat(result).isEqualTo(PlanApplyResult.Disabled)
            assertThat(harness.activeSnapshot()).isNull()
            assertThat(harness.core.alarmScheduler.isScheduled(armed.sessionId)).isFalse()
            assertThat(harness.planStore.current.enabled).isFalse()
        }

    /** Décision Q9 du 2026-10-24 : avant le coucher, le nouveau fuseau donne le réveil à son heure locale. */
    @Test
    fun aNewTimeZoneBeforeBedtimeRearmsAtTheLocalTimeOfTheNewZone() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val first = harness.activeSnapshot()!!
            harness.timeZoneProvider.zoneId = "Europe/London"

            val result = harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.TIMEZONE_CHANGED)

            assertThat(result).isInstanceOf(PlanApplyResult.Armed::class.java)
            val second = harness.activeSnapshot()!!
            assertThat(second.sessionId).isNotEqualTo(first.sessionId)
            assertThat(second.wakeSchedule.triggerAtEpochMillis)
                .isEqualTo(epochMillisAt("2026-10-23T07:00", zoneId = "Europe/London"))
            assertThat(second.wakeSchedule.zoneIdAtActivation).isEqualTo("Europe/London")
        }

    /** Décision du 2026-10-24 : une resynchronisation qui ne change aucun instant ne désarme rien. */
    @Test
    fun aClockChangeThatKeepsTheSameInstantsChangesNothing() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val armed = harness.activeSnapshot()!!
            harness.core.clock.now += 2_000

            val result = harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.TIME_CHANGED)

            assertThat(result).isEqualTo(PlanApplyResult.Unchanged)
            assertThat(harness.activeSnapshot()?.sessionId).isEqualTo(armed.sessionId)
            assertThat(harness.finalStatesBySession()).hasSize(1)
        }

    /** Décision Q14 du 2026-10-24 : l'échec laisse l'interrupteur allumé et se lit dans [PlanArmingStatus]. */
    @Test
    fun aFailedArmingKeepsTheSwitchOnAndIsObservable() =
        runTest {
            val harness = PlanTestHarness(plan = WEEKDAYS_PLAN.copy(enabled = false))
            harness.core.alarmScheduler.scheduleResult = OperationResult.Failure("ANDROID_ALARM_SCHEDULE_FAILED")

            val result = harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)

            val failure = (result as PlanApplyResult.Failed).failure
            assertThat(failure).isInstanceOf(ActivationFailure.CoordinatorFailed::class.java)
            assertThat(harness.planStore.current.enabled).isTrue()
            assertThat(harness.activeSnapshot()).isNull()
            assertThat(harness.armingStatus.state.value).isInstanceOf(PlanArmingState.Failed::class.java)
            assertThat(harness.core.technicalEventLog.logged).contains(TechnicalEventType.PLAN_REARM_FAILED)
        }

    @Test
    fun aSuccessfulArmingClearsAPreviousFailure() =
        runTest {
            val harness = PlanTestHarness(plan = WEEKDAYS_PLAN.copy(enabled = false))
            harness.core.alarmScheduler.scheduleResult = OperationResult.Failure("ANDROID_ALARM_SCHEDULE_FAILED")
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            harness.core.alarmScheduler.scheduleResult = OperationResult.Success

            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)

            assertThat(harness.armingStatus.state.value).isEqualTo(PlanArmingState.Ok)
            assertThat(harness.activeSnapshot()?.state).isEqualTo(SessionStateDto.ARMED)
        }

    /** Un planning invalide est refusé avant tout désarmement : la session en attente reste armée. */
    @Test
    fun anInvalidPlanIsRefusedBeforeDisarmingAnything() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val armed = harness.activeSnapshot()!!

            val thrown =
                runCatching {
                    harness.scheduler.apply(
                        WEEKDAYS_PLAN.copy(wakeTimeIso = "7h"),
                        PlanChangeReason.WAKE_TIME,
                    )
                }

            assertThat(thrown.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(harness.activeSnapshot()?.sessionId).isEqualTo(armed.sessionId)
            assertThat(harness.activeSnapshot()?.state).isEqualTo(SessionStateDto.ARMED)
        }

    /** Avant déverrouillage, le planning ne s'écrit pas : refus avant tout désarmement. */
    @Test
    fun anApplyBeforeUnlockIsRefusedBeforeDisarmingAnything() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            harness.core.unlockState.isUserUnlocked = false

            val thrown =
                runCatching { harness.scheduler.apply(WEEKDAYS_PLAN.copy(enabled = false), PlanChangeReason.SWITCH) }

            assertThat(thrown.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            assertThat(harness.activeSnapshot()?.state).isEqualTo(SessionStateDto.ARMED)
        }
}
