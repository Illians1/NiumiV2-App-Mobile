package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.system.common.OperationResult
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome
import com.niumi.system.session.SessionPersistenceGateway

/**
 * `CLEAR_ACTIVE_SESSION`, best-effort (SPEC_CORE_KMP §6). Efface le pointeur Room et le snapshot
 * Direct Boot ; ne touche pas au dernier snapshot publié par `SessionSnapshotPublisher`, que
 * l'écran de fin de session doit encore pouvoir lire.
 */
class ClearActiveSessionExecutor(
    private val gateway: SessionPersistenceGateway,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        gateway.clearActive(snapshot.sessionId)
        return ExecutionOutcome(OperationResult.Success)
    }
}
