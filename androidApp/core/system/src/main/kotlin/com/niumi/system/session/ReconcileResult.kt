package com.niumi.system.session

import com.niumi.core.interop.IncidentSeverityDto

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

    data object PointerCleared : ReconcileAction
}

data class ReconcileResult(
    val sessionId: String?,
    val actions: List<ReconcileAction>,
)
