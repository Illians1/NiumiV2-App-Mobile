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

    data object SnapshotCorrupted : ReconcileAction

    data object PointerCleared : ReconcileAction
}

data class ReconcileResult(
    val sessionId: String?,
    val actions: List<ReconcileAction>,
)
