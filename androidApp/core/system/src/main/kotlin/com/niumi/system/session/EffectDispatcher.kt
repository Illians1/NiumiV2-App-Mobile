package com.niumi.system.session

import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import com.niumi.system.common.OperationResult

data class EffectOutcome(
    val effect: PendingEffect,
    val result: OperationResult,
)

/** Vue agrégée des résultats d'une exécution, par nature d'effet (utilisé par [PhaseCompletion]). */
data class EffectOutcomes(
    val outcomes: List<EffectOutcome>,
) {
    fun succeeded(kind: SessionEffectKindDto): Boolean =
        outcomes.filter { it.effect.kind == kind }.all { it.result !is OperationResult.Failure }

    fun failureCodeOf(kind: SessionEffectKindDto): String? =
        outcomes
            .firstOrNull { it.effect.kind == kind && it.result is OperationResult.Failure }
            ?.let { (it.result as OperationResult.Failure).code }
}

data class EffectExecutionResult(
    val outcomes: EffectOutcomes,
    val incidents: List<SessionIncidentDto>,
)

/**
 * Table `SessionEffectKind → EffectExecutor` (SPEC_CORE_KMP §6). Exécute [effects] dans l'ordre
 * reçu — celui de la décision pour une exécution fraîche, `revision ASC, ordinal ASC` pour un
 * rejeu d'outbox (déjà fourni par [SessionPersistenceGateway.pendingEffects]) — et marque chaque
 * effet dans l'outbox aussitôt après son exécution. `AlreadySatisfied` est marqué `SATISFIED` et
 * compte comme un succès (règle d'échappement, SPEC_CORE_KMP §6 dernier alinéa).
 */
class EffectDispatcher(
    private val executors: Map<SessionEffectKindDto, EffectExecutor>,
    private val gateway: SessionPersistenceGateway,
) {
    suspend fun execute(
        effects: List<PendingEffect>,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): EffectExecutionResult {
        val outcomes = mutableListOf<EffectOutcome>()
        val incidents = mutableListOf<SessionIncidentDto>()
        for (effect in effects) {
            val executor = executors.getValue(effect.kind)
            val execution = executor.execute(effect, snapshot, extras)
            val status =
                when (execution.result) {
                    is OperationResult.Success -> EffectStatus.SUCCEEDED
                    is OperationResult.AlreadySatisfied -> EffectStatus.SATISFIED
                    is OperationResult.Failure -> EffectStatus.FAILED
                }
            gateway.markEffect(effect.effectId, status, (execution.result as? OperationResult.Failure)?.code)
            outcomes += EffectOutcome(effect, execution.result)
            execution.incident?.let { incidents += it }
        }
        return EffectExecutionResult(EffectOutcomes(outcomes), incidents)
    }
}
