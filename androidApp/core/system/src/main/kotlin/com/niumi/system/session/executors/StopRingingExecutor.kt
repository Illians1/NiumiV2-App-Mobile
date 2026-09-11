package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.system.ringing.RingingController
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/**
 * `STOP_RINGING`, requis pour `RELEASE_SUCCEEDED` (décision validée le 2026-09-10, voir
 * [com.niumi.system.session.PhaseCompletion]).
 */
class StopRingingExecutor(
    private val ringingController: RingingController,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome = ExecutionOutcome(ringingController.stopRinging(snapshot.sessionId))
}
