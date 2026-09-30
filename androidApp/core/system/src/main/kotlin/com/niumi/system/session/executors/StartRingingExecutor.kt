package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
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
 *
 * **`RINGING_STARTED` n'est pas journalisé ici** (2026-09-29) : lancer le service ne dit pas que le
 * son a démarré. Seul le service le sait, et il est le seul producteur de l'événement
 * (`RingingStartJournal`). Les deux l'écrivaient, soit deux événements pour une sonnerie.
 */
class StartRingingExecutor(
    private val ringingController: RingingController,
    private val ringingWatchdog: RingingWatchdog,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        val result = ringingController.startRinging(snapshot.sessionId, snapshot.revision)
        if (result is OperationResult.Success) {
            ringingWatchdog.arm(snapshot.sessionId)
        }
        return ExecutionOutcome(result)
    }
}
