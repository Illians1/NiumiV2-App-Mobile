package com.niumi.database

import androidx.room.withTransaction
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.dao.BlockedAppDao
import com.niumi.database.dao.OutboxDao
import com.niumi.database.directboot.UnlockState
import com.niumi.database.entity.ActiveSessionPointerEntity
import com.niumi.database.entity.AlarmSessionEntity
import com.niumi.database.mapping.toBlockedPackage
import com.niumi.database.mapping.toDomain
import com.niumi.database.mapping.toEntity
import com.niumi.database.mapping.toExtras
import com.niumi.database.mapping.toPendingEffect
import com.niumi.database.mapping.toSnapshotDto
import javax.inject.Provider

private const val ROOM_BEFORE_UNLOCK_MESSAGE = "ROOM_BEFORE_UNLOCK"

/**
 * Implémentation Room de [SessionStore] (SPEC_CORE_KMP §13). Chaque décision est écrite dans une
 * seule transaction (`database.withTransaction`, `room-ktx`) : session, applications bloquées,
 * pointeur, reçu et effets. `@Transaction` sur un DAO est écarté ici : il ne peut appeler que des
 * méthodes de son propre DAO, ce qui imposerait un DAO unique pour cinq tables.
 *
 * [databaseProvider] plutôt qu'un [NiumiDatabase] direct : SPEC_ANDROID §7.3 interdit d'ouvrir
 * Room avant `UserManager.isUserUnlocked == true`. [database] vérifie [UnlockState] et lève avant
 * même d'appeler `databaseProvider.get()` — la base n'est donc jamais construite si l'appareil
 * est verrouillé, pas seulement inutilisée (`RoomSessionStoreUnlockGuardTest`, `ETAPE-10.md`).
 */
class RoomSessionStore(
    private val databaseProvider: Provider<NiumiDatabase>,
    private val unlockState: UnlockState,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : SessionStore {
    private val database: NiumiDatabase
        get() {
            check(unlockState.isUserUnlocked) { ROOM_BEFORE_UNLOCK_MESSAGE }
            return databaseProvider.get()
        }

    private val sessionDao get() = database.sessionDao()
    private val blockedAppDao get() = database.blockedAppDao()
    private val pointerDao get() = database.activeSessionPointerDao()
    private val receiptDao get() = database.receiptDao()
    private val outboxDao get() = database.outboxDao()
    private val incidentDao get() = database.incidentDao()

    override suspend fun activeSession(): StoredSession? = database.withTransaction { loadActiveSession() }

    override suspend fun commitDecision(decision: StoredDecision) {
        database.withTransaction { writeDecision(decision) }
    }

    override suspend fun findReceipt(eventId: String): EventReceipt? = receiptDao.findByEventId(eventId)?.toDomain()

    override suspend fun receipts(sessionId: String): List<EventReceipt> =
        receiptDao.forSession(sessionId).map { it.toDomain() }

    override suspend fun pendingEffects(sessionId: String): List<PendingEffect> =
        outboxDao.replayable(sessionId).map { it.toPendingEffect() }

    override suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) {
        outboxDao.updateStatus(effectId, status, error, nowEpochMillis())
    }

    override suspend fun clearActivePointer(sessionId: String) {
        pointerDao.clear(sessionId)
    }

    override suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ) {
        incidentDao.insert(incident.toEntity(sessionId))
    }

    private suspend fun loadActiveSession(): StoredSession? =
        pointerDao
            .current()
            ?.sessionId
            ?.let { sessionDao.findById(it) }
            ?.let { entity -> toStoredSession(entity, blockedAppDao, outboxDao) }

    private suspend fun writeDecision(decision: StoredDecision) {
        val existing = sessionDao.findById(decision.snapshot.sessionId)
        val extras = decision.androidExtras.freezeFrom(existing)
        sessionDao.upsert(decision.snapshot.toEntity(extras))
        blockedAppDao.replaceForSession(
            decision.snapshot.sessionId,
            extras.blockedPackages.map { it.toEntity(decision.snapshot.sessionId) },
        )
        pointerDao.set(ActiveSessionPointerEntity(sessionId = decision.snapshot.sessionId))
        receiptDao.insert(decision.receipt.toEntity())
        outboxDao.insertAll(decision.effects.map { it.toEntity(decision.receipt.receivedAtEpochMillis) })
    }
}

// Fonctions de fichier plutôt que membres de `RoomSessionStore` : la classe est au plafond
// detekt `TooManyFunctions` (11) depuis l'ajout de `receipts`/`recordIncident` à l'étape 11.
private suspend fun toStoredSession(
    entity: AlarmSessionEntity,
    blockedAppDao: BlockedAppDao,
    outboxDao: OutboxDao,
): StoredSession =
    StoredSession(
        snapshot = entity.toSnapshotDto(),
        extras = entity.toExtras(blockedAppDao.forSession(entity.id).map { it.toBlockedPackage() }),
        pendingEffects = outboxDao.replayable(entity.id).map { it.toPendingEffect() },
    )

/**
 * `boxId`, `boxTokenSha256Hex`, `ringtoneKey` et `vibrationEnabled` sont figés à l'activation
 * (SPEC_ANDROID §7.2, dernier alinéa) : une session déjà présente en base fait toujours foi sur
 * ces quatre champs, jamais l'appelant. `blockedPackages` n'est pas concerné par cette garantie à
 * cette étape (voir ETAPE-09.md).
 */
private fun AndroidSessionExtras.freezeFrom(existing: AlarmSessionEntity?): AndroidSessionExtras =
    if (existing == null) {
        this
    } else {
        copy(
            boxId = existing.boxId,
            boxTokenSha256Hex = existing.boxTokenSha256Hex,
            ringtoneKey = existing.ringtoneKey,
            vibrationEnabled = existing.vibrationEnabled,
        )
    }
