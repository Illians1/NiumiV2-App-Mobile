package com.niumi.system.session

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionSnapshotDto

/**
 * Décision(s) prise(s) par une réconciliation (non défini par le plan MVP : nécessaire pour que
 * `SessionReconcilerTest` assère une décision plutôt qu'un effet de bord). Une passe peut combiner
 * plusieurs actions, dans l'ordre où elles ont été exécutées : rejeu de l'outbox d'abord, nouvelle
 * décision ensuite (SPEC_CORE_KMP §6.1, SPEC_ANDROID §9.2, §9.3).
 */
sealed interface ReconcileAction {
    data class OutboxReplayed(
        val effectCount: Int,
    ) : ReconcileAction

    data class DecisionApplied(
        val dispatchResult: DispatchResult,
    ) : ReconcileAction

    data class AlarmRescheduled(
        val triggerAtEpochMillis: Long,
    ) : ReconcileAction

    /**
     * Alarme de début du blocage reposée au **même** instant contractuel (Lot 6, §9.3, §12.4) :
     * jamais recalculée depuis l'heure locale, `startsAtEpochMillis` étant immuable après
     * l'activation (SPEC_CORE_KMP §8.3).
     */
    data class BlockingStartRescheduled(
        val startsAtEpochMillis: Long,
    ) : ReconcileAction

    data class IncidentDispatched(
        val code: String,
        val severity: IncidentSeverityDto,
    ) : ReconcileAction

    /** Sonnerie relancée sur une session `RINGING` dont le service avait disparu (§10.2). */
    data object RingingResumed : ReconcileAction

    /**
     * Nettoyage encore incomplet après le rejeu : [effectCount] effets **requis** de la libération
     * restent `PENDING`/`FAILED`, la session demeure donc `RELEASING` (SPEC_ANDROID §11.3 : « la
     * session n'est pas présentée comme terminée avant `RELEASE_SUCCEEDED` »). Aucun
     * `RELEASE_FAILED` n'est redispatché : le coordinateur l'a déjà fait au moment de l'échec.
     */
    data class ReleaseStillPending(
        val effectCount: Int,
    ) : ReconcileAction

    /** Demande de scan republiée sur une session en attente de scan (§10.5). */
    data object ScanRequestRepublished : ReconcileAction

    data object SnapshotCorrupted : ReconcileAction

    /**
     * Pointeur d'une session finie effacé par la passe. [snapshot] est l'état final : le
     * `WakePlanScheduler` en tire les conséquences quand elles n'ont pas pu l'être au moment de la
     * libération (appareil verrouillé, processus mort entre les deux ; étape 44).
     */
    data class PointerCleared(
        val snapshot: SessionSnapshotDto,
    ) : ReconcileAction

    /** Le planning a armé une session ([triggerAtEpochMillis] : son réveil), étape 44. */
    data class PlanRearmed(
        val triggerAtEpochMillis: Long,
    ) : ReconcileAction

    /** Le planning est allumé mais l'armement a échoué ([code] : `ActivationFailure.diagnosticCode`). */
    data class PlanRearmFailed(
        val code: String,
    ) : ReconcileAction

    /** Une annulation (`CANCELLED`) a éteint l'interrupteur (`design-plan/SPECIFICATION.md` §11, §13). */
    data object PlanSwitchedOff : ReconcileAction
}

data class ReconcileResult(
    val sessionId: String?,
    val actions: List<ReconcileAction>,
)
