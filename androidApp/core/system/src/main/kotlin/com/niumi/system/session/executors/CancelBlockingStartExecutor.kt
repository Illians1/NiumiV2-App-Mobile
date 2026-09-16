package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.system.alarm.BlockingStartScheduler
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/**
 * `CANCEL_BLOCKING_START`, best-effort (SPEC_CORE_KMP §6) : son échec est consigné mais ne bloque
 * jamais la libération. Un déclenchement orphelin est absorbé par `BlockingStartHandler`, dont le
 * moteur refuse l'événement faute de session `ARMED` en attente (SPEC_ANDROID §12.4).
 *
 * Le moteur ne produit cet effet que pour une session à blocage différé : une session immédiate n'a
 * jamais posé d'alarme de début, et l'annuler serait sans objet (décision du 2026-09-16,
 * `ETAPE-22.md`). L'annulation reste néanmoins idempotente — `AlreadySatisfied` sans `PendingIntent`.
 */
class CancelBlockingStartExecutor(
    private val scheduler: BlockingStartScheduler,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome = ExecutionOutcome(scheduler.cancel(snapshot.sessionId))
}
