package com.niumi.system.session

import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.system.common.OperationResult

/**
 * Résultat de [SessionPersistenceGateway.load]. `Unreadable` distingue un snapshot Direct Boot
 * corrompu de l'absence de session (SPEC_CORE_KMP §13 : « aucune suppression silencieuse du
 * blocage à cause d'un snapshot illisible ») — un `Corrupted` ne doit jamais se lire « pas de
 * session ».
 */
sealed interface LoadResult {
    data object Absent : LoadResult

    data class Present(
        val snapshot: SessionSnapshotDto,
        val extras: AndroidSessionExtras,
        val pendingEffects: List<PendingEffect>,
    ) : LoadResult

    data class Unreadable(
        val reason: String,
    ) : LoadResult
}

/**
 * Persistance vue par le coordinateur (« Interfaces transverses » du plan MVP, étendue à l'étape
 * 11 avec `recordIncident`). Room après déverrouillage, Direct Boot sinon — voir
 * [UnlockAwarePersistenceGateway].
 */
interface SessionPersistenceGateway {
    suspend fun load(): LoadResult

    suspend fun commit(decision: StoredDecision)

    suspend fun receipt(eventId: String): EventReceipt?

    suspend fun pendingEffects(sessionId: String): List<PendingEffect>

    suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    )

    suspend fun clearActive(sessionId: String)

    // Avant déverrouillage, le snapshot Direct Boot n'a pas de table d'incidents : renvoie
    // `Failure("INCIDENT_DEFERRED_UNTIL_UNLOCK")`, best-effort, rejoué à `USER_UNLOCKED`.
    suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ): OperationResult
}
