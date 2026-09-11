package com.niumi.system.session.executors

import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.system.blocking.BlockingController
import com.niumi.system.common.Clock
import com.niumi.system.common.OperationResult
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/**
 * `REMOVE_BLOCKING`, requis pour `RELEASE_SUCCEEDED` (SPEC_CORE_KMP §6, §12 ; SPEC_ANDROID §11.3).
 * Règle d'échappement : si le service d'accessibilité est déjà désactivé, le retrait est considéré
 * satisfait (`AlreadySatisfied`) plutôt que de bloquer indéfiniment `RELEASING`, et un incident
 * `BLOCKING_PERMISSION_REVOKED`/`CRITICAL` est collecté pour que le coordinateur le journalise.
 */
class RemoveBlockingExecutor(
    private val blockingController: BlockingController,
    private val clock: Clock,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        val result = blockingController.remove(snapshot.sessionId)
        val incident =
            if (result is OperationResult.AlreadySatisfied && !blockingController.isServiceEnabled()) {
                SessionIncidentDto(
                    code = IncidentCodes.BLOCKING_PERMISSION_REVOKED,
                    severity = IncidentSeverityDto.CRITICAL,
                    occurredAtEpochMillis = clock.nowEpochMillis(),
                    platform = PlatformDto.ANDROID,
                )
            } else {
                null
            }
        return ExecutionOutcome(result, incident)
    }
}
