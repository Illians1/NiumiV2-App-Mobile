package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.isImmediate
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventDetails
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.blocking.BlockingController
import com.niumi.system.common.OperationResult
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/**
 * `APPLY_BLOCKING`, requis pour `ACTIVATION_SUCCEEDED` (SPEC_CORE_KMP §6, §12.2).
 *
 * Sur une session à blocage **différé**, l'exécution réussie journalise en plus `BLOCKING_STARTED`
 * (SPEC_ANDROID §17, Lot 6) : les `BLOCK_APPLIED` par package suivent, comme à l'activation, mais
 * seul ce type dit que le blocage a **commencé à l'heure choisie** plutôt qu'à l'armement. Le
 * critère est le schedule, pas l'état ni la révision : une activation dont l'instant de début est
 * déjà dépassé produit elle aussi `APPLY_BLOCKING` d'emblée (§8.3), et c'est bien un début différé.
 *
 * **« Réussie » inclut `AlreadySatisfied`** (SPEC_CORE_KMP §6 : un effet satisfait compte comme
 * réussi). Défaut mesuré le 2026-09-24 sur appareil, trois sessions différées sur trois : quand le
 * service d'accessibilité tourne, `PUBLISH_PLATFORM_SNAPSHOT`, premier effet de la même décision,
 * fait rafraîchir la projection par `BlockingProjectionRefresher`, qui la lit déjà active — le
 * snapshot commité porte `blockingAppliedAtEpochMillis`. Cet exécuteur recevait alors
 * `AlreadySatisfied` et ne journalisait rien : le blocage avait commencé sans laisser de trace. Les
 * tests instrumentés ne pouvaient pas le voir, le service n'y tournant jamais. Aucun doublon n'est
 * possible au rejeu : un effet `SUCCEEDED` ou `SATISFIED` n'est jamais réexécuté.
 */
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
        if (result !is OperationResult.Failure) {
            if (!snapshot.blockingSchedule.isImmediate) {
                technicalEventLog.log(TechnicalEventType.BLOCKING_STARTED, snapshot.sessionId)
            }
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
