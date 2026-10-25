package com.niumi.system.plan

import com.google.common.truth.Truth.assertThat
import com.niumi.core.diagnostics.ActivationReasonCode
import com.niumi.core.interop.NextOccurrenceDto
import com.niumi.core.interop.NextOccurrenceInputDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakePlanDto
import com.niumi.database.BlockedPackage
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.common.OperationResult
import com.niumi.system.plan.fakes.InMemoryAlarmSoundPreferences
import com.niumi.system.plan.fakes.InMemoryAppSelectionStore
import com.niumi.system.plan.fakes.PARIS
import com.niumi.system.plan.fakes.epochMillisAt
import com.niumi.system.readiness.fakes.FakePairedBoxStore
import com.niumi.system.session.LoadResult
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Armement d'une occurrence calculée (SPEC_ANDROID §9.2 depuis l'étape 44 ; SPEC_CORE_KMP §10).
 * Les contrôles de préparation ne sont plus transmis à la politique commune : l'interrupteur
 * s'active toujours (`design-plan/SPECIFICATION.md` §2), seules les bornes communes refusent
 * (Niumi Spots, sélection, réveil dans le futur). Ce qui manque à l'appareil se signale ailleurs
 * (alerte et bandeau, étape 52).
 */
class ArmOccurrenceUseCaseTest {
    private val harness = TestCoordinatorHarness().apply { clock.now = epochMillisAt("2026-10-22T20:00") }
    private val pairedBoxStore = FakePairedBoxStore()
    private val appSelectionStore = InMemoryAppSelectionStore()
    private val alarmSoundPreferences = InMemoryAlarmSoundPreferences()

    private val useCase =
        ArmOccurrenceUseCase(
            harness.facade,
            harness.eventFactory,
            pairedBoxStore,
            appSelectionStore,
            alarmSoundPreferences,
        )

    /** Lundi à vendredi, 7:00, coucher 22:30 ; jeudi 22 octobre 2026 à 20:00 → vendredi 7:00. */
    private fun occurrence(
        bedtimeIso: String = "22:30",
        nowEpochMillis: Long = harness.clock.now,
    ): NextOccurrenceDto =
        requireNotNull(
            harness.facade
                .computeNextOccurrence(
                    NextOccurrenceInputDto(
                        WakePlanDto(setOf(1, 2, 3, 4, 5), "07:00", bedtimeIso),
                        PARIS,
                        nowEpochMillis,
                    ),
                ).occurrence,
        )

    private suspend fun arm(occurrence: NextOccurrenceDto = occurrence()): ArmSessionResult =
        harness.coordinator.withLock { useCase.arm(occurrence, it) }

    @Test
    fun theOccurrenceIsArmedAtItsComputedInstants() =
        runTest {
            val result = arm()

            val snapshot = (result as ArmSessionResult.Armed).snapshot
            assertThat(snapshot.state).isEqualTo(SessionStateDto.ARMED)
            assertThat(snapshot.wakeSchedule.triggerAtEpochMillis).isEqualTo(epochMillisAt("2026-10-23T07:00"))
            assertThat(snapshot.blockingSchedule.startsAtEpochMillis).isEqualTo(epochMillisAt("2026-10-22T22:30"))
            assertThat(harness.blockingStartScheduler.isScheduled(snapshot.sessionId)).isTrue()
        }

    /**
     * Coucher déjà passé : le blocage commence tout de suite (§2). L'armement ne demande rien :
     * l'accueil demande confirmation avant (décision de Mehdi du 2026-10-25).
     */
    @Test
    fun anOccurrenceWhoseBedtimeHasPassedBlocksImmediately() =
        runTest {
            val result = arm(occurrence(bedtimeIso = "19:00"))

            val snapshot = (result as ArmSessionResult.Armed).snapshot
            assertThat(snapshot.blockingAppliedAtEpochMillis).isNotNull()
            assertThat(harness.journal.calls).contains("BlockingController.apply")
        }

    /** Lus au moment d'armer, jamais transportés : la sonnerie et la sélection de cet instant. */
    @Test
    fun theSoundAndTheSelectionAreReadAtArmingTime() =
        runTest {
            alarmSoundPreferences.settings = AlarmSoundSettings("niumi_oiseaux", 300)
            appSelectionStore.stored = listOf(BlockedPackage("com.example.video", "Vidéo"))

            val result = arm()

            val sessionId = (result as ArmSessionResult.Armed).snapshot.sessionId
            val extras = (harness.gateway.load() as LoadResult.Present).extras
            assertThat(sessionId).isNotEmpty()
            assertThat(extras.ringtoneKey).isEqualTo("niumi_oiseaux")
            assertThat(extras.volumeRampSeconds).isEqualTo(300)
            assertThat(extras.blockedPackages).containsExactly(BlockedPackage("com.example.video", "Vidéo"))
        }

    @Test
    fun noNiumiSpotRefusesWithoutAnySession() =
        runTest {
            pairedBoxStore.clear()

            val result = arm()

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.Blocked
            assertThat(failure.reasons.map { it.code }).contains(ActivationReasonCode.NO_PAIRED_BOX)
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
        }

    @Test
    fun anEmptySelectionRefusesWithoutAnySession() =
        runTest {
            appSelectionStore.stored = emptyList()

            val result = arm()

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.Blocked
            assertThat(failure.reasons.map { it.code }).contains(ActivationReasonCode.INVALID_APP_SELECTION)
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
        }

    /** Une occurrence calculée trop tôt n'est jamais armée dans le passé. */
    @Test
    fun anOccurrenceAlreadyInThePastIsRefused() =
        runTest {
            val stale = occurrence()
            harness.clock.now = epochMillisAt("2026-10-23T08:00")

            val result = arm(stale)

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.Blocked
            assertThat(failure.reasons.map { it.code }).contains(ActivationReasonCode.TRIGGER_NOT_IN_FUTURE)
        }

    /** L'alarme exacte refusée par Android : `ACTIVATION_FAILED`, aucun pointeur actif. */
    @Test
    fun anAlarmThatCannotBeScheduledEndsInACoordinatorFailure() =
        runTest {
            harness.alarmScheduler.scheduleResult = OperationResult.Failure("ANDROID_ALARM_SCHEDULE_FAILED")

            val result = arm()

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.CoordinatorFailed
            assertThat(failure.state).isEqualTo(SessionStateDto.FAILED)
            assertThat(failure.failureCode).isEqualTo("ANDROID_ALARM_SCHEDULE_FAILED")
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
        }
}
