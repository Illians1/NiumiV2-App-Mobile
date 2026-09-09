package com.niumi.database

import com.niumi.core.interop.SessionSnapshotDto

/** Session active reconstruite depuis Room (« Interfaces transverses » du plan MVP). */
data class StoredSession(
    val snapshot: SessionSnapshotDto,
    val extras: AndroidSessionExtras,
    val pendingEffects: List<PendingEffect>,
)

/**
 * Décision à écrire atomiquement (SPEC_CORE_KMP §6.1, §12 ; SPEC_ANDROID §9.2 étape 4) : snapshot,
 * reçu de l'événement et effets de la décision, dans une seule transaction Room.
 */
data class StoredDecision(
    val snapshot: SessionSnapshotDto,
    val receipt: EventReceipt,
    val effects: List<PendingEffect>,
    val androidExtras: AndroidSessionExtras,
)

/**
 * Persistance canonique Android après déverrouillage (SPEC_CORE_KMP §13). Implémentée par
 * `RoomSessionStore`. Toutes les opérations sont `suspend` : jamais d'accès Room sur le thread
 * appelant.
 */
interface SessionStore {
    suspend fun activeSession(): StoredSession?

    suspend fun commitDecision(decision: StoredDecision) // une seule transaction Room

    suspend fun findReceipt(eventId: String): EventReceipt?

    suspend fun pendingEffects(sessionId: String): List<PendingEffect>

    suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    )

    suspend fun clearActivePointer(sessionId: String)
}
