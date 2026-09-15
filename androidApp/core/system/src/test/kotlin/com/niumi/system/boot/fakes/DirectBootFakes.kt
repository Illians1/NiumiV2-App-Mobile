package com.niumi.system.boot.fakes

import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.SessionStore
import com.niumi.database.SessionStoreUnreadableException
import com.niumi.database.StoredDecision
import com.niumi.database.StoredSession
import com.niumi.database.directboot.DirectBootMergeResult
import com.niumi.database.directboot.DirectBootRoomMerge
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.DirectBootWriteResult
import com.niumi.database.directboot.UnlockState

private val REPLAYABLE_STATUSES = setOf(EffectStatus.PENDING, EffectStatus.FAILED)

/** `UnlockState` pilotable : verrouillé par défaut, comme au sortir d'un redémarrage. */
class FakeUnlockState(
    override var isUserUnlocked: Boolean = false,
) : UnlockState

/**
 * `DirectBootStore` en mémoire. Rejoue la garde de révision de `decideWrite` — scopée par
 * `sessionId`, une nouvelle session repartant légitimement plus bas (SPEC_ANDROID §7.3) — parce
 * qu'elle est `internal` à `:core:database` et que la fusion doit être jugée contre le même refus
 * qu'en production.
 */
class InMemoryDirectBootStore(
    private var snapshot: DirectBootSnapshot? = null,
) : DirectBootStore {
    var writeCount: Int = 0
        private set
    var clearCount: Int = 0
        private set

    override fun read(): DirectBootSnapshot? = snapshot

    override fun write(snapshot: DirectBootSnapshot.Active): DirectBootWriteResult {
        writeCount++
        val existing = this.snapshot
        if (existing is DirectBootSnapshot.Active &&
            existing.sessionId == snapshot.sessionId &&
            snapshot.domainRevision < existing.domainRevision
        ) {
            return DirectBootWriteResult.StaleRevision
        }
        this.snapshot = snapshot
        return DirectBootWriteResult.Written
    }

    override fun clear() {
        clearCount++
        snapshot = null
    }

    fun seed(snapshot: DirectBootSnapshot?) {
        this.snapshot = snapshot
    }
}

/**
 * `DirectBootRoomMerge` qui n'écrit rien mais retient ce qu'on lui a demandé de fusionner : la
 * transaction Room elle-même est prouvée par `RoomDirectBootMergeTest` (instrumenté), pas ici.
 */
class RecordingDirectBootRoomMerge(
    var result: DirectBootMergeResult =
        DirectBootMergeResult.Merged(
            sessionAdvanced = false,
            receiptsInserted = 0,
            effectsInserted = 0,
            effectsAdvanced = 0,
        ),
    /**
     * Base illisible (étape 20). Aucune doublure ne levait jusqu'ici, et c'est précisément ce qui
     * a laissé passer le plantage mesuré sur appareil le 2026-09-15 : `RoomDirectBootMerge` touche
     * Room avant le `gateway.load()` de la passe.
     */
    var failure: SessionStoreUnreadableException? = null,
) : DirectBootRoomMerge {
    val merged: MutableList<DirectBootSnapshot.Active> = mutableListOf()

    override suspend fun merge(projection: DirectBootSnapshot.Active): DirectBootMergeResult {
        failure?.let { throw it }
        merged += projection
        return result
    }
}

/**
 * `SessionStore` en mémoire, réduit à ce dont la fusion a besoin : la réécriture de la projection
 * lit la session active, ses reçus et ses effets rejouables.
 */
class InMemorySessionStore : SessionStore {
    private var snapshot: SessionSnapshotDto? = null
    private var extras: AndroidSessionExtras? = null
    private val receipts = mutableListOf<EventReceipt>()
    private val effects = mutableMapOf<String, PendingEffect>()
    val incidents: MutableList<Pair<String, SessionIncidentDto>> = mutableListOf()

    fun seed(
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
        receipts: List<EventReceipt> = emptyList(),
        effects: List<PendingEffect> = emptyList(),
    ) {
        this.snapshot = snapshot
        this.extras = extras
        this.receipts.clear()
        this.receipts += receipts
        this.effects.clear()
        effects.forEach { this.effects[it.effectId] = it }
    }

    override suspend fun activeSession(): StoredSession? {
        val current = snapshot ?: return null
        return extras?.let { StoredSession(current, it, pendingEffects(current.sessionId)) }
    }

    override suspend fun commitDecision(decision: StoredDecision) {
        snapshot = decision.snapshot
        extras = decision.androidExtras
        receipts += decision.receipt
        decision.effects.forEach { effects[it.effectId] = it }
    }

    override suspend fun findReceipt(eventId: String): EventReceipt? = receipts.firstOrNull { it.eventId == eventId }

    override suspend fun receipts(sessionId: String): List<EventReceipt> = receipts.filter { it.sessionId == sessionId }

    override suspend fun pendingEffects(sessionId: String): List<PendingEffect> =
        effects.values
            .filter { it.sessionId == sessionId && it.status in REPLAYABLE_STATUSES }
            .sortedWith(compareBy({ it.revision }, { it.ordinal }))

    override suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) {
        effects[effectId]?.let { effects[effectId] = it.copy(status = status, lastError = error) }
    }

    override suspend fun clearActivePointer(sessionId: String) {
        snapshot = null
        extras = null
    }

    override suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ) {
        incidents += sessionId to incident
    }
}
