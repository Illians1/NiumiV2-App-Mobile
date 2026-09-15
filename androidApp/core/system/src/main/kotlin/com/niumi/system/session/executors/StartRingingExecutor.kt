package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.RingingWatchdog
import com.niumi.system.common.OperationResult
import com.niumi.system.ringing.RingingController
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/**
 * `START_RINGING`, best-effort (SPEC_CORE_KMP §6). Arme l'alarme de secours (SPEC_ANDROID §10.2,
 * étape 20) dès que le service a effectivement démarré : la première réconciliation sur `RINGING`
 * la réarmera de toute façon (`SessionReconciler`), mais armer dès l'entrée dans l'état couvre la
 * fenêtre entre les deux sans attendre un premier tic.
 */
class StartRingingExecutor(
    private val ringingController: RingingController,
    private val ringingWatchdog: RingingWatchdog,
    private val technicalEventLog: TechnicalEventLog,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        val result = ringingController.startRinging(snapshot.sessionId, snapshot.revision)
        if (result is OperationResult.Success) {
            ringingWatchdog.arm(snapshot.sessionId)
            technicalEventLog.log(TechnicalEventType.RINGING_STARTED, snapshot.sessionId)
        }
        return ExecutionOutcome(result)
    }
}
