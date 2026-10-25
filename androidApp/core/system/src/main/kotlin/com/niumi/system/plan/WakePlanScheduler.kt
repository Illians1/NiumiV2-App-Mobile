package com.niumi.system.plan

/**
 * Seul chemin par lequel le planning arme, désarme et ré-arme une session (étape 44 ; décision 3 du
 * Lot 13 du plan de refonte). Tout se passe sous le verrou du coordinateur : aucun événement ne
 * s'intercale entre le désarmement d'une session en attente et l'armement de la suivante, et
 * l'interface n'écrit jamais un état de session.
 */
interface WakePlanScheduler {
    /**
     * Enregistre [plan] et l'applique :
     * - session en attente (avant le coucher) : désarmée, puis la prochaine occurrence est armée ;
     * - du coucher au scan : [PlanApplyResult.LockedBySession], rien n'est enregistré ;
     * - interrupteur éteint : désarmement seul ;
     * - aucun jour coché : réveil ponctuel à la prochaine heure de réveil, éteint après le scan du
     *   matin ; heures identiques : période de 24 h (décisions du 2026-10-25).
     */
    suspend fun apply(
        plan: WakePlan,
        reason: PlanChangeReason,
    ): PlanApplyResult
}
