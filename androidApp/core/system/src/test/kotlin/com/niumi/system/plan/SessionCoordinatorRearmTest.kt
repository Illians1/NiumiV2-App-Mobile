package com.niumi.system.plan

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.plan.fakes.PlanTestHarness
import com.niumi.system.plan.fakes.WEEKDAYS_PLAN
import com.niumi.system.plan.fakes.epochMillisAt
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.ReconcileAction
import com.niumi.system.session.ReconcileReason
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Ce qui suit la fin d'une session, sous le verrou du coordinateur (étape 44) : le réveil terminé
 * arme l'occurrence suivante (`design-plan/SPECIFICATION.md` §15 : retour à « Réveil prévu ») ;
 * l'annulation éteint l'interrupteur (§11, §13) ; la sortie de secours suit la même règle (D2).
 */
class SessionCoordinatorRearmTest {
    /** Vendredi 23 octobre, réveil terminé : lundi 26 (passage à l'heure d'hiver le 25). */
    private val mondaySeven = epochMillisAt("2026-10-26T07:00")

    @Test
    fun aScanDuringTheAlarmArmsTheNextOccurrence() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            val first = harness.ring()

            val result = harness.scan()

            assertThat((result as DispatchResult.Applied).snapshot?.state).isEqualTo(SessionStateDto.COMPLETED)
            val next = harness.activeSnapshot()!!
            assertThat(next.sessionId).isNotEqualTo(first.sessionId)
            assertThat(next.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(next.wakeSchedule.triggerAtEpochMillis).isEqualTo(mondaySeven)
        }

    /** « Ponctuel » (2026-10-25) : une fois le réveil terminé, l'interrupteur s'éteint et rien n'est armé. */
    @Test
    fun aOneOffAlarmTurnsTheSwitchOffOnceCompleted() =
        runTest {
            val harness = PlanTestHarness()
            val oneOff = WEEKDAYS_PLAN.copy(enabledIsoDays = emptySet())
            harness.scheduler.apply(oneOff, PlanChangeReason.SWITCH)
            harness.ring()

            val result = harness.scan()

            assertThat((result as DispatchResult.Applied).snapshot?.state).isEqualTo(SessionStateDto.COMPLETED)
            assertThat(harness.activeSnapshot()).isNull()
            assertThat(harness.planStore.current).isEqualTo(oneOff.copy(enabled = false))
        }

    /** La recopie du matin consomme aussi un réveil ponctuel. */
    @Test
    fun aOneOffAlarmEndedByTheEmergencyExitTurnsTheSwitchOff() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN.copy(enabledIsoDays = emptySet()), PlanChangeReason.SWITCH)
            harness.ring()

            harness.emergencyExit()

            assertThat(harness.activeSnapshot()).isNull()
            assertThat(harness.planStore.current.enabled).isFalse()
        }

    /** D2 (2026-10-24) : le matin, la recopie termine la session et laisse le planning allumé. */
    @Test
    fun anEmergencyExitDuringTheAlarmArmsTheNextOccurrence() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            harness.ring()

            harness.emergencyExit()

            assertThat(harness.activeSnapshot()?.wakeSchedule?.triggerAtEpochMillis).isEqualTo(mondaySeven)
            assertThat(harness.planStore.current.enabled).isTrue()
        }

    @Test
    fun aCancellationTurnsTheSwitchOffAndArmsNothing() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN.copy(bedtimeIso = "19:00"), PlanChangeReason.SWITCH)

            val result = harness.scan()

            assertThat((result as DispatchResult.Applied).snapshot?.state).isEqualTo(SessionStateDto.CANCELLED)
            assertThat(harness.activeSnapshot()).isNull()
            assertThat(harness.planStore.current.enabled).isFalse()
            assertThat(harness.planStore.current.bedtimeIso).isEqualTo("19:00")
        }

    /** D2 : le soir, la recopie annule la session et éteint l'interrupteur. */
    @Test
    fun anEmergencyExitInTheEveningTurnsTheSwitchOff() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN.copy(bedtimeIso = "19:00"), PlanChangeReason.SWITCH)

            harness.emergencyExit()

            assertThat(harness.activeSnapshot()).isNull()
            assertThat(harness.planStore.current.enabled).isFalse()
        }

    /** Une session ponctuelle de l'écran 6 transitoire (planning éteint) ne réarme rien. */
    @Test
    fun aCompletedSessionWithTheSwitchOffArmsNothing() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            harness.planStore.write(WEEKDAYS_PLAN.copy(enabled = false))
            harness.ring()

            harness.scan()

            assertThat(harness.activeSnapshot()).isNull()
        }

    /**
     * Point de vigilance 6 : jamais de ré-armement avant déverrouillage, le planning y est illisible.
     * Le déverrouillage le rattrape.
     */
    @Test
    fun aSessionFinishedOnALockedDeviceIsRearmedOnlyAfterUnlock() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            harness.ring()
            harness.core.unlockState.isUserUnlocked = false

            harness.scan()
            assertThat(harness.activeSnapshot()).isNull()

            harness.core.unlockState.isUserUnlocked = true
            val result = harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(result.actions).contains(ReconcileAction.PlanRearmed(mondaySeven))
            assertThat(harness.activeSnapshot()?.state).isEqualTo(SessionStateDto.ARMED)
        }

    /**
     * Désarmement interrompu (alarme non annulée) : la session reste `RELEASING`, rien n'est armé
     * par-dessus. Le moteur a déjà fixé la cible `DISARMED` : le nouveau planning est donc enregistré,
     * et c'est lui que la reprise de l'outbox arme une fois le désarmement refermé.
     */
    @Test
    fun aDisarmCompletedByTheReconcilerArmsTheNewPlan() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            harness.core.alarmScheduler.cancelResult = OperationResult.Failure("ANDROID_CANCEL_FAILED")

            val interrupted =
                harness.scheduler.apply(
                    WEEKDAYS_PLAN.copy(wakeTimeIso = "06:30"),
                    PlanChangeReason.WAKE_TIME,
                )
            assertThat(interrupted).isInstanceOf(PlanApplyResult.Failed::class.java)
            assertThat(harness.activeSnapshot()?.state).isEqualTo(SessionStateDto.RELEASING)
            assertThat(harness.planStore.current.wakeTimeIso).isEqualTo("06:30")

            harness.core.alarmScheduler.cancelResult = OperationResult.Success
            harness.coordinator.reconcile(ReconcileReason.FOREGROUND)

            val next = harness.activeSnapshot()!!
            assertThat(next.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(next.wakeSchedule.triggerAtEpochMillis).isEqualTo(epochMillisAt("2026-10-23T06:30"))
        }

    /** Même interruption sur une extinction : l'interrupteur reste éteint, rien n'est réarmé. */
    @Test
    fun anInterruptedSwitchOffNeverRearms() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN, PlanChangeReason.SWITCH)
            harness.core.alarmScheduler.cancelResult = OperationResult.Failure("ANDROID_CANCEL_FAILED")

            harness.scheduler.apply(WEEKDAYS_PLAN.copy(enabled = false), PlanChangeReason.SWITCH)
            assertThat(harness.planStore.current.enabled).isFalse()
            assertThat(harness.armingStatus.state.value).isEqualTo(PlanArmingState.Ok)

            harness.core.alarmScheduler.cancelResult = OperationResult.Success
            harness.coordinator.reconcile(ReconcileReason.FOREGROUND)

            assertThat(harness.activeSnapshot()).isNull()
        }

    /**
     * La libération est déjà enregistrée quand le crochet s'exécute : un échec du planning (ici
     * l'écriture qui lève) ne doit jamais remonter à l'appelant du scan, qui croirait la session
     * toujours en cours. L'échec est journalisé.
     */
    @Test
    fun aFailingPlanStoreNeverTurnsTheReleaseIntoAnException() =
        runTest {
            val harness = PlanTestHarness()
            harness.scheduler.apply(WEEKDAYS_PLAN.copy(bedtimeIso = "19:00"), PlanChangeReason.SWITCH)
            harness.planStore.writeFails = true

            val result = harness.scan()

            assertThat((result as DispatchResult.Applied).snapshot?.state).isEqualTo(SessionStateDto.CANCELLED)
            assertThat(harness.core.technicalEventLog.logged).contains(TechnicalEventType.PLAN_REARM_FAILED)
        }
}
