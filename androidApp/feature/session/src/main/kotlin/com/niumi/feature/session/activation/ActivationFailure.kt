package com.niumi.feature.session.activation

import com.niumi.core.interop.ActivationReasonDto
import com.niumi.core.interop.DomainViolationDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleStatusDto

/**
 * Issues de `ArmSessionUseCase.arm` (SPEC_ANDROID §9.2). Une seule d'entre elles autorise un
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
