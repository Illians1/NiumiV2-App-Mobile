package com.niumi.system.plan

import com.niumi.core.interop.ActivationReasonDto
import com.niumi.core.interop.BlockingScheduleStatusDto
import com.niumi.core.interop.DomainViolationDto
import com.niumi.core.interop.NextOccurrenceStatusDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleStatusDto

/**
 * Issues de `ArmOccurrenceUseCase.arm` et de `ArmSessionUseCase.arm` (SPEC_ANDROID §9.2). Déplacées
 * de `:feature:session` à l'étape 44 : le `WakePlanScheduler` (`:core:system`) les renvoie dans
 * `PlanApplyResult.Failed` et les publie dans `PlanArmingStatus`. Une seule d'entre elles autorise un
 * réessai immédiat sans que rien n'ait changé côté utilisateur : [CoordinatorFailed], dont
 * `ACTIVATION_FAILED` a déjà effacé le pointeur de session actif (`CLEAR_ACTIVE_SESSION`,
 * SPEC_CORE_KMP §10). [Rejected] et [Duplicate] signalent au contraire un état qui ne se
 * résoudra pas en réessayant à l'identique.
 */
sealed interface ActivationFailure {
    /** Point 2 de §9.2 : refus avant tout dispatch, un contrôle bloquant ou une borne commune. */
    data class Blocked(
        val reasons: List<ActivationReasonDto>,
    ) : ActivationFailure

    /** `computeWakeSchedule` n'a produit aucun horaire pour l'heure locale demandée. */
    data class InvalidSchedule(
        val status: WakeScheduleStatusDto,
    ) : ActivationFailure

    /**
     * `computeBlockingSchedule` a refusé le début de blocage demandé (Lot 6). Le cas courant est
     * `NOT_BEFORE_TRIGGER` : la règle de « prochaine occurrence » fait tomber l'heure choisie après le
     * réveil, et l'utilisateur doit alors choisir un blocage immédiat (SPEC_CORE_KMP §8.3). Distinct
     * de [Blocked] : rien ne cloche sur l'appareil, c'est le choix qui est impossible.
     */
    data class InvalidBlockingSchedule(
        val status: BlockingScheduleStatusDto,
    ) : ActivationFailure

    /**
     * `computeNextOccurrence` n'a produit aucune occurrence armable (étape 44) : heure illisible,
     * fuseau inconnu ou période vidée par le changement d'heure de printemps (`EMPTY_PERIOD`).
     */
    data class InvalidPlan(
        val status: NextOccurrenceStatusDto,
    ) : ActivationFailure

    /** Le coordinateur a enchaîné `ACTIVATION_FAILED` : le pointeur actif a été effacé, réessai sûr. */
    data class CoordinatorFailed(
        val failureCode: String?,
        val state: SessionStateDto?,
    ) : ActivationFailure

    /** Le moteur commun a refusé l'événement : aucune session n'a été créée. */
    data class Rejected(
        val violations: List<DomainViolationDto>,
    ) : ActivationFailure

    /** `eventId` déjà reçu : ne jamais revendiquer `ARMED` sur cette seule base. */
    data object Duplicate : ActivationFailure
}

sealed interface ArmSessionResult {
    data class Armed(
        val snapshot: SessionSnapshotDto,
    ) : ArmSessionResult

    data class Failed(
        val failure: ActivationFailure,
    ) : ArmSessionResult
}

/**
 * Code court d'un échec, pour le journal technique (`PLAN_REARM_FAILED`, SPEC_ANDROID §17) : un code
 * contrôlé, jamais un message libre.
 */
val ActivationFailure.diagnosticCode: String
    get() =
        when (this) {
            is ActivationFailure.Blocked -> reasons.firstOrNull()?.code ?: "ACTIVATION_BLOCKED"
            is ActivationFailure.InvalidSchedule -> "WAKE_SCHEDULE_${status.name}"
            is ActivationFailure.InvalidBlockingSchedule -> "BLOCKING_SCHEDULE_${status.name}"
            is ActivationFailure.InvalidPlan -> "NEXT_OCCURRENCE_${status.name}"
            is ActivationFailure.CoordinatorFailed -> failureCode ?: "ANDROID_ACTIVATION_FAILED"
            is ActivationFailure.Rejected -> violations.firstOrNull()?.code ?: "ACTIVATION_REJECTED"
            ActivationFailure.Duplicate -> "EVENT_ID_DUPLICATE"
        }
