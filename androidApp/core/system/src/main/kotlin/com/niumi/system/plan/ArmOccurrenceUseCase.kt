package com.niumi.system.plan

import com.niumi.core.interop.ActivationPolicyInputDto
import com.niumi.core.interop.ActivationRequestDto
import com.niumi.core.interop.AppSelectionSummaryDto
import com.niumi.core.interop.NextOccurrenceDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.LockedSession
import com.niumi.system.session.SessionEventFactory

/**
 * Arme une occurrence déjà calculée (SPEC_ANDROID §9.2 depuis l'étape 44 ; SPEC_CORE_KMP §8.4, §10).
 * Remplace la partie « dispatch » d'`ArmSessionUseCase` : le `WakePlanScheduler` l'appelle sous le
 * verrou du coordinateur, et l'écran 6 transitoire aussi, jusqu'à l'étape 45.
 *
 * **Aucun contrôle de préparation n'est transmis** (`checks = emptyList()`, SPEC_CORE_KMP §10) :
 * l'interrupteur s'active toujours (`design-plan/SPECIFICATION.md` §2). Restent les bornes communes —
 * un à dix Niumi Spots, une à cinquante applications, réveil dans le futur — et les échecs des effets
 * requis, qui mènent à `FAILED` sans pointeur actif. Une permission manquante se signale par l'alerte
 * et le bandeau (étape 52), ou par l'échec de l'alarme exacte, jamais par un refus silencieux ici.
 *
 * Sonnerie, sélection et Niumi Spots sont lus **au moment d'armer**, jamais transportés.
 */
class ArmOccurrenceUseCase(
    private val facade: NiumiCoreFacade,
    private val eventFactory: SessionEventFactory,
    private val pairedBoxStore: PairedBoxStore,
    private val appSelectionStore: AppSelectionStore,
    private val alarmSoundPreferences: AlarmSoundPreferences,
) {
    suspend fun arm(
        occurrence: NextOccurrenceDto,
        session: LockedSession,
    ): ArmSessionResult {
        val pairedBoxCount = pairedBoxStore.all().size
        val selection = appSelectionStore.selection()
        val policy =
            facade.evaluateActivation(
                ActivationPolicyInputDto(
                    checks = emptyList(),
                    appSelectionCount = selection.size,
                    triggerAtEpochMillis = occurrence.wakeSchedule.triggerAtEpochMillis,
                    nowEpochMillis = eventFactory.nowEpochMillis(),
                    pairedBoxCount = pairedBoxCount,
                    blockingStartsAtEpochMillis = occurrence.blockingSchedule.startsAtEpochMillis,
                ),
            )
        if (!policy.allowed) return ArmSessionResult.Failed(ActivationFailure.Blocked(policy.blockingReasons))

        // `sanitized()` protège d'une clé ou d'une durée retirée du catalogue depuis la dernière
        // écriture de la préférence (point de vigilance 4 du plan de refonte).
        val alarmSound = alarmSoundPreferences.read().sanitized()
        val request =
            ActivationRequestDto(
                wakeSchedule = occurrence.wakeSchedule,
                appSelection = AppSelectionSummaryDto(count = selection.size),
                blockingSchedule = occurrence.blockingSchedule,
            )
        val extras =
            AndroidSessionExtras(
                ringtoneKey = alarmSound.ringtoneKey,
                vibrationEnabled = true,
                volumeRampSeconds = alarmSound.volumeRampSeconds,
                blockedPackages = selection,
            )

        return when (val result = session.dispatch(eventFactory.activationRequested(request), extras)) {
            is DispatchResult.Applied -> interpretApplied(result)
            is DispatchResult.Rejected -> ArmSessionResult.Failed(ActivationFailure.Rejected(result.violations))
            is DispatchResult.Duplicate -> ArmSessionResult.Failed(ActivationFailure.Duplicate)
        }
    }

    private fun interpretApplied(applied: DispatchResult.Applied): ArmSessionResult {
        val snapshot = applied.snapshot
        return if (snapshot != null && snapshot.state == SessionStateDto.ARMED) {
            ArmSessionResult.Armed(snapshot)
        } else {
            ArmSessionResult.Failed(ActivationFailure.CoordinatorFailed(snapshot?.failureCode, snapshot?.state))
        }
    }
}
