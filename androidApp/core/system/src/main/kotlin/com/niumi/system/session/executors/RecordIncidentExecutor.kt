package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.database.mapping.SessionEffectMapper
import com.niumi.system.common.OperationResult
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome
import com.niumi.system.session.SessionPersistenceGateway

private const val MISSING_PAYLOAD_CODE = "MISSING_INCIDENT_PAYLOAD"

/**
 * `RECORD_INCIDENT`, best-effort (SPEC_CORE_KMP §6). Seul effet à porter un `payload` non nul ;
 * avant déverrouillage, `gateway.recordIncident` renvoie `Failure("INCIDENT_DEFERRED_UNTIL_UNLOCK")`
 * (aucune table d'incidents dans le snapshot Direct Boot), rejoué à `USER_UNLOCKED`.
 */
class RecordIncidentExecutor(
    private val gateway: SessionPersistenceGateway,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        val payloadJson = effect.payloadJson ?: return ExecutionOutcome(OperationResult.Failure(MISSING_PAYLOAD_CODE))
        val incident = SessionEffectMapper.decodeIncidentPayload(payloadJson).incident
        return ExecutionOutcome(gateway.recordIncident(snapshot.sessionId, incident))
    }
}
