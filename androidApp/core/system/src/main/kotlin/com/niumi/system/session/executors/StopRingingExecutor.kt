package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.system.alarm.RingingWatchdog
import com.niumi.system.ringing.RingingController
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/**
 * `STOP_RINGING`, requis pour `RELEASE_SUCCEEDED` (décision validée le 2026-09-10, voir
 * [com.niumi.system.session.PhaseCompletion]). Désarme l'alarme de secours (SPEC_ANDROID §10.2,
 * étape 20) **avant** d'arrêter le service : si `stopRinging` échoue, l'alarme de secours doit
 * malgré tout disparaître — sinon un tic ultérieur relancerait un son que la session a quitté.
 */
class StopRingingExecutor(
    private val ringingController: RingingController,
    private val ringingWatchdog: RingingWatchdog,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        ringingWatchdog.disarm(snapshot.sessionId)
        return ExecutionOutcome(ringingController.stopRinging(snapshot.sessionId))
    }
}
