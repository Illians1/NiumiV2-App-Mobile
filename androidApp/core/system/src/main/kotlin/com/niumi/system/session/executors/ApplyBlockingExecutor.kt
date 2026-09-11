package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventDetails
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.blocking.BlockingController
import com.niumi.system.common.OperationResult
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/** `APPLY_BLOCKING`, requis pour `ACTIVATION_SUCCEEDED` (SPEC_CORE_KMP §6, §12.2). */
class ApplyBlockingExecutor(
    private val blockingController: BlockingController,
    private val technicalEventLog: TechnicalEventLog,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        val result = blockingController.apply(snapshot.sessionId, extras.blockedPackages.toSet())
        if (result is OperationResult.Success) {
            extras.blockedPackages.forEach {
                technicalEventLog.log(
                    TechnicalEventType.BLOCK_APPLIED,
                    snapshot.sessionId,
                    TechnicalEventDetails.packageName(it.packageName),
                )
            }
        }
        return ExecutionOutcome(result)
    }
}
