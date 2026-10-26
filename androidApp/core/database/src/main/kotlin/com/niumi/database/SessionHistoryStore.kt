package com.niumi.database

import android.database.sqlite.SQLiteException
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.directboot.UnlockState
import com.niumi.database.mapping.toSnapshotDto
import javax.inject.Provider

private const val ROOM_BEFORE_UNLOCK_MESSAGE = "ROOM_BEFORE_UNLOCK"

/**
 * Relit une session par son identifiant, active ou finie (étape 46). Les lignes `alarm_session` ne
 * sont jamais supprimées : une session `COMPLETED` se relit encore après l'effacement du pointeur
 * actif et l'armement de la session suivante, que le crochet du planning fait sous le même verrou
 * (écran 11, feuille d'annulation ; SPEC_ANDROID §11.3).
 */
fun interface SessionHistoryStore {
    suspend fun find(sessionId: String): SessionSnapshotDto?
}

/**
 * Implémentation Room de [SessionHistoryStore]. Classe distincte de [RoomSessionStore], au plafond
 * detekt `TooManyFunctions`. Même garde de déverrouillage : la base n'est jamais construite avant
 * `isUserUnlocked` (SPEC_ANDROID §7.3).
 */
class RoomSessionHistoryStore(
    private val databaseProvider: Provider<NiumiDatabase>,
    private val unlockState: UnlockState,
) : SessionHistoryStore {
    // `SQLiteException` seule, comme `RoomSessionStore.activeSession` : la garde de déverrouillage
    // reste un défaut de programmation qui remonte tel quel.
    override suspend fun find(sessionId: String): SessionSnapshotDto? {
        check(unlockState.isUserUnlocked) { ROOM_BEFORE_UNLOCK_MESSAGE }
        return try {
            databaseProvider
                .get()
                .sessionDao()
                .findById(sessionId)
                ?.toSnapshotDto()
        } catch (exception: SQLiteException) {
            throw SessionStoreUnreadableException(exception.message ?: "ROOM_UNREADABLE", exception)
        }
    }
}
