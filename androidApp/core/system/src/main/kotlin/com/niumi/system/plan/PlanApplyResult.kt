package com.niumi.system.plan

import com.niumi.core.interop.NextOccurrenceDto

/** Issue de [WakePlanScheduler.apply] (étape 44). */
sealed interface PlanApplyResult {
    /** Une session `ARMED` existe pour [occurrence]. */
    data class Armed(
        val occurrence: NextOccurrenceDto,
    ) : PlanApplyResult

    /** Interrupteur éteint : la session en attente éventuelle a été désarmée, rien n'est armé. */
    data object Disabled : PlanApplyResult

    /**
     * Du coucher au scan, rien ne se modifie (SPEC_CORE_KMP §2, décision 4) : la session est laissée
     * telle quelle et le planning reçu n'est pas enregistré.
     */
    data object LockedBySession : PlanApplyResult

    /**
     * Horloge ou fuseau déplacés sans changer les instants de la session en attente : rien n'est
     * désarmé (une resynchronisation réseau ne doit pas créer de session `DISARMED`).
     */
    data object Unchanged : PlanApplyResult

    /** Armement impossible : l'interrupteur reste allumé (décision Q14 du 2026-10-24). */
    data class Failed(
        val failure: ActivationFailure,
    ) : PlanApplyResult
}
