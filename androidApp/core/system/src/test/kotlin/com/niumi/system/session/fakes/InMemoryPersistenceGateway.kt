package com.niumi.system.session.fakes

import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.system.common.OperationResult
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway

private val REPLAYABLE_STATUSES = setOf(EffectStatus.PENDING, EffectStatus.FAILED)

private data class ActiveSession(
    val snapshot: SessionSnapshotDto,
    val extras: AndroidSessionExtras,
)

/**
 * [SessionPersistenceGateway] entièrement en mémoire, sans Room ni Direct Boot — les scénarios de
 * l'étape 11 portent sur le coordinateur, pas sur la persistance déjà prouvée aux étapes 9-10.
 * Journalise `commit`/`markEffect`/`clearActive` dans [journal] pour les tests d'ordre.
 */
class InMemoryPersistenceGateway(
    private val journal: CallJournal,
) : SessionPersistenceGateway {
    private var active: ActiveSession? = null
    private val receiptsByEventId = mutableMapOf<String, EventReceipt>()
    private val effectsById = mutableMapOf<String, PendingEffect>()
    val incidentsRecorded = mutableListOf<Pair<String, SessionIncidentDto>>()
    var incidentRecordingAllowed: Boolean = true
    var forceUnreadable: String? = null

    override suspend fun load(): LoadResult {
        val unreadableReason = forceUnreadable
        val session = active
        return when {
            unreadableReason != null -> LoadResult.Unreadable(unreadableReason)
            session == null -> LoadResult.Absent
            else -> LoadResult.Present(session.snapshot, session.extras, replayable(session.snapshot.sessionId))
        }
    }

    override suspend fun commit(decision: StoredDecision) {
        journal.record("gateway.commit")
        active = ActiveSession(decision.snapshot, decision.androidExtras)
        receiptsByEventId[decision.receipt.eventId] = decision.receipt
        decision.effects.forEach { effectsById[it.effectId] = it }
    }

    override suspend fun receipt(eventId: String): EventReceipt? = receiptsByEventId[eventId]

    override suspend fun pendingEffects(sessionId: String): List<PendingEffect> = replayable(sessionId)

    override suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) {
        journal.record("gateway.markEffect")
        effectsById[effectId]?.let { effectsById[effectId] = it.copy(status = status, lastError = error) }
    }

    override suspend fun clearActive(sessionId: String) {
        journal.record("gateway.clearActive")
        active = null
    }

    override suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ): OperationResult {
        journal.record("gateway.recordIncident")
        return if (incidentRecordingAllowed) {
            incidentsRecorded += sessionId to incident
            OperationResult.Success
        } else {
            OperationResult.Failure("INCIDENT_DEFERRED_UNTIL_UNLOCK")
        }
    }

    private fun replayable(sessionId: String): List<PendingEffect> =
        effectsById.values
            .filter { it.sessionId == sessionId && it.status in REPLAYABLE_STATUSES }
            .sortedWith(compareBy({ it.revision }, { it.ordinal }))
}
