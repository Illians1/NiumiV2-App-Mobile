package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.BlockingStartScheduler
import com.niumi.system.common.OperationResult
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/**
 * `SCHEDULE_BLOCKING_START`, requis pour `ACTIVATION_SUCCEEDED` d'une session à blocage différé
 * (SPEC_CORE_KMP §6 ; SPEC_ANDROID §9.2 point 6, §12.4, §18). Son échec pendant `PREPARING` fait
 * échouer l'activation, exactement comme celui de `SCHEDULE_ALARM`.
 */
class ScheduleBlockingStartExecutor(
    private val scheduler: BlockingStartScheduler,
    private val technicalEventLog: TechnicalEventLog,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        // Cas impossible par construction : le moteur ne produit cet effet que pour un blocage
        // différé, dont les trois champs sont renseignés (SPEC_CORE_KMP §7.5). Il est écrit tout de
        // même — `!!` est interdit par detekt, et un échec typé vaut mieux qu'une exception qui
        // ferait tomber toute la phase d'activation sans code d'erreur exploitable. Même patron que
        // `RecordIncidentExecutor` sur son payload manquant.
        val startsAt =
            snapshot.blockingSchedule.startsAtEpochMillis
                ?: return ExecutionOutcome(OperationResult.Failure("BLOCKING_START_WITHOUT_SCHEDULE"))

        val result = scheduler.schedule(snapshot.sessionId, snapshot.revision, startsAt)
        if (result is OperationResult.Success) {
            technicalEventLog.log(TechnicalEventType.BLOCKING_SCHEDULED, snapshot.sessionId)
        }
        return ExecutionOutcome(result)
    }
}
