package com.niumi.system.session

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.SessionHistoryStore
import com.niumi.database.SessionStoreUnreadableException
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.database.directboot.toSnapshotDto

/**
 * Relit une session par son identifiant, finie comprise (étape 46). Après le scan du matin, le
 * crochet du planning arme la session suivante sous le même verrou : [SessionSnapshotPublisher]
 * passe de `COMPLETED` à la nouvelle session `ARMED` en quelques millisecondes. L'écran de réveil
 * et la feuille d'annulation suivent donc *leur* session par identifiant et la relisent ici
 * (SPEC_ANDROID §11.3).
 */
fun interface SessionHistoryReader {
    suspend fun find(sessionId: String): SessionSnapshotDto?
}

/**
 * Room après déverrouillage, projection Direct Boot avant — et seulement si elle porte la session
 * demandée : la projection ne garde que la session active (SPEC_ANDROID §7.3). Une base illisible
 * se lit « introuvable ».
 */
class UnlockAwareSessionHistoryReader(
    private val historyStore: SessionHistoryStore,
    private val directBootStore: DirectBootStore,
    private val unlockState: UnlockState,
) : SessionHistoryReader {
    override suspend fun find(sessionId: String): SessionSnapshotDto? =
        if (unlockState.isUserUnlocked) {
            // Base illisible (SPEC_ANDROID §18) : introuvable, jamais une exception jusqu'à l'écran.
            try {
                historyStore.find(sessionId)
            } catch (_: SessionStoreUnreadableException) {
                null
            }
        } else {
            (directBootStore.read() as? DirectBootSnapshot.Active)
                ?.takeIf { it.sessionId == sessionId }
                ?.toSnapshotDto()
        }
}
