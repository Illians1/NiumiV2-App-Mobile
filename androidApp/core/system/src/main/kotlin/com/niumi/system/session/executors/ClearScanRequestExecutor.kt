package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.notification.ScanRequestNotifier
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/** `CLEAR_SCAN_REQUEST`, best-effort (SPEC_CORE_KMP §6, SPEC_ANDROID §10.5). */
class ClearScanRequestExecutor(
    private val notifier: ScanRequestNotifier,
    private val technicalEventLog: TechnicalEventLog,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        val result = notifier.clear(snapshot.sessionId)
        if (result is OperationResult.Success) {
            technicalEventLog.log(TechnicalEventType.SCAN_REQUEST_CLEARED, snapshot.sessionId)
        }
        return ExecutionOutcome(result)
    }
}
