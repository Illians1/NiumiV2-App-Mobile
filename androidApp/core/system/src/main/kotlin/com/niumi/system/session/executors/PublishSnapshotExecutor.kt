package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome
import com.niumi.system.session.SessionSnapshotPublisher

/**
 * `PUBLISH_PLATFORM_SNAPSHOT` (SPEC_CORE_KMP §6) : premier effet de toute décision, exécuté à
 * chaque transition d'état. Point unique pour journaliser les six types §17 liés à une transition
 * d'état (`SESSION_PREPARING`, `SESSION_ARMED`, `SESSION_RELEASING`, `SESSION_CANCELLED`,
 * `SESSION_COMPLETED`, `SESSION_FAILED`) : aucun autre exécuteur ne connaît systématiquement
 * `snapshot.state` au moment de chaque transition.
 */
class PublishSnapshotExecutor(
    private val publisher: SessionSnapshotPublisher,
    private val technicalEventLog: TechnicalEventLog,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        publisher.publish(snapshot)
        stateLogType(snapshot.state)?.let { technicalEventLog.log(it, snapshot.sessionId) }
        return ExecutionOutcome(OperationResult.Success)
    }

    private fun stateLogType(state: SessionStateDto): TechnicalEventType? =
        when (state) {
            SessionStateDto.PREPARING -> TechnicalEventType.SESSION_PREPARING

            SessionStateDto.ARMED -> TechnicalEventType.SESSION_ARMED

            SessionStateDto.RELEASING -> TechnicalEventType.SESSION_RELEASING

            SessionStateDto.CANCELLED -> TechnicalEventType.SESSION_CANCELLED

            SessionStateDto.COMPLETED -> TechnicalEventType.SESSION_COMPLETED

            SessionStateDto.FAILED -> TechnicalEventType.SESSION_FAILED

            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            -> null
        }
}
