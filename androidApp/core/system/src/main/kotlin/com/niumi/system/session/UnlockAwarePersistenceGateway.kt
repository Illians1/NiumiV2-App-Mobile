package com.niumi.system.session

import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.SessionStore
import com.niumi.database.StoredDecision
import com.niumi.database.directboot.DirectBootMapper
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.database.directboot.toExtras
import com.niumi.database.directboot.toPendingEffects
import com.niumi.database.directboot.toReceipts
import com.niumi.database.directboot.toSnapshotDto
import com.niumi.system.common.OperationResult

private val DIRECT_BOOT_REPLAYABLE_STATUSES = setOf(EffectStatus.PENDING, EffectStatus.FAILED)
private const val INCIDENT_DEFERRED_CODE = "INCIDENT_DEFERRED_UNTIL_UNLOCK"

/**
 * Room après déverrouillage, Direct Boot avant (SPEC_CORE_KMP §13, SPEC_ANDROID §7.3, §9.2). Après
 * chaque écriture Room, la même session est reprojetée dans Direct Boot ; l'inverse (Room ← Direct
 * Boot) est la fusion à `USER_UNLOCKED`, hors périmètre de cette étape (étape 19). Un `write()`
 * refusé côté Direct Boot (`StaleRevision`, `Failed`) est ignoré ici sans propager d'erreur : §9.2
 * dit explicitement que Room fait foi et que `SessionReconciler` réécrit la projection.
 */
class UnlockAwarePersistenceGateway(
    private val sessionStore: SessionStore,
    private val directBootStore: DirectBootStore,
    private val unlockState: UnlockState,
) : SessionPersistenceGateway {
    override suspend fun load(): LoadResult =
        if (unlockState.isUserUnlocked) {
            sessionStore.activeSession()?.let { stored ->
                LoadResult.Present(stored.snapshot, stored.extras, stored.pendingEffects)
            } ?: LoadResult.Absent
        } else {
            when (val snapshot = directBootStore.read()) {
                null -> {
                    LoadResult.Absent
                }

                is DirectBootSnapshot.Corrupted -> {
                    LoadResult.Unreadable(snapshot.reason)
                }

                is DirectBootSnapshot.Active -> {
                    LoadResult.Present(snapshot.toSnapshotDto(), snapshot.toExtras(), replayableEffectsOf(snapshot))
                }
            }
        }

    override suspend fun commit(decision: StoredDecision) {
        if (unlockState.isUserUnlocked) {
            sessionStore.commitDecision(decision)
            mirrorActiveSession()
        } else {
            writeDirectBoot(decision)
        }
    }

    override suspend fun receipt(eventId: String): EventReceipt? =
        if (unlockState.isUserUnlocked) {
            sessionStore.findReceipt(eventId)
        } else {
            activeDirectBootSnapshot()?.toReceipts()?.firstOrNull { it.eventId == eventId }
        }

    override suspend fun pendingEffects(sessionId: String): List<PendingEffect> =
        if (unlockState.isUserUnlocked) {
            sessionStore.pendingEffects(sessionId)
        } else {
            activeDirectBootSnapshot()
                ?.takeIf { it.sessionId == sessionId }
                ?.let { replayableEffectsOf(it) }
                .orEmpty()
        }

    override suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) {
        if (unlockState.isUserUnlocked) {
            sessionStore.markEffect(effectId, status, error)
            mirrorActiveSession()
        } else {
            updateDirectBootEffect(effectId, status, error)
        }
    }

    override suspend fun clearActive(sessionId: String) {
        if (unlockState.isUserUnlocked) {
            sessionStore.clearActivePointer(sessionId)
        }
        directBootStore.clear()
    }

    override suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ): OperationResult =
        if (unlockState.isUserUnlocked) {
            sessionStore.recordIncident(sessionId, incident)
            mirrorActiveSession()
            OperationResult.Success
        } else {
            OperationResult.Failure(INCIDENT_DEFERRED_CODE)
        }

    private fun activeDirectBootSnapshot(): DirectBootSnapshot.Active? =
        directBootStore.read() as? DirectBootSnapshot.Active

    private suspend fun mirrorActiveSession() {
        val stored = sessionStore.activeSession() ?: return
        directBootStore.write(
            DirectBootMapper.projectionOf(
                snapshot = stored.snapshot,
                extras = stored.extras,
                receipts = sessionStore.receipts(stored.snapshot.sessionId),
                effects = sessionStore.pendingEffects(stored.snapshot.sessionId),
            ),
        )
    }

    private fun writeDirectBoot(decision: StoredDecision) {
        val existing = activeDirectBootSnapshot()?.takeIf { it.sessionId == decision.snapshot.sessionId }
        val existingReceipts = existing?.toReceipts().orEmpty()
        val existingEffects = existing?.toPendingEffects().orEmpty()
        val mergedEffects =
            existingEffects.filterNot { old -> decision.effects.any { it.effectId == old.effectId } } + decision.effects
        directBootStore.write(
            DirectBootMapper.projectionOf(
                snapshot = decision.snapshot,
                extras = decision.androidExtras,
                receipts = existingReceipts + decision.receipt,
                effects = mergedEffects,
            ),
        )
    }

    private fun updateDirectBootEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) {
        val existing = activeDirectBootSnapshot() ?: return
        val updatedEffects =
            existing.toPendingEffects().map { effect ->
                if (effect.effectId == effectId) effect.copy(status = status, lastError = error) else effect
            }
        directBootStore.write(
            DirectBootMapper.projectionOf(
                snapshot = existing.toSnapshotDto(),
                extras = existing.toExtras(),
                receipts = existing.toReceipts(),
                effects = updatedEffects,
            ),
        )
    }
}

// Fonction de fichier plutôt que membre de la classe : au plafond detekt `TooManyFunctions` (11)
// depuis l'ajout de `recordIncident` à l'étape 11.
private fun replayableEffectsOf(snapshot: DirectBootSnapshot.Active): List<PendingEffect> =
    snapshot.toPendingEffects().filter { it.status in DIRECT_BOOT_REPLAYABLE_STATUSES }
