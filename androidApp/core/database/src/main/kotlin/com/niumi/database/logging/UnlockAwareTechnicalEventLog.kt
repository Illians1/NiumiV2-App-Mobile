package com.niumi.database.logging

import com.niumi.database.directboot.UnlockState
import javax.inject.Inject
import javax.inject.Provider

private const val MAX_ENTRIES = 200

/**
 * Route [log] vers [RoomTechnicalEventLog] une fois l'appareil déverrouillé, vers
 * [InMemoryTechnicalEventLog] avant (SPEC_ANDROID §7.3 : Room y est inaccessible). Un seul
 * appel écrit dans une seule destination, jamais les deux : aucun doublon possible. [roomLog] est
 * un [Provider] pour ne jamais construire `NiumiDatabase` avant que [UnlockState.isUserUnlocked]
 * ne soit vrai — `ROOM_BEFORE_UNLOCK` (`RoomSessionStore`) protège Room lui-même, ce `Provider`
 * évite en plus de résoudre le binding avant d'en avoir besoin.
 *
 * [recent] fusionne les deux sources et trie par [TechnicalEventEntry.occurredAtEpochMillis]
 * décroissant : `InMemoryTechnicalEventLog.recent()` et `RoomTechnicalEventLog.recent()` ont des
 * ordres internes différents (croissant pour l'un, décroissant pour l'autre, voir leurs classes) —
 * ni l'un ni l'autre n'a de contrat d'ordre documenté aujourd'hui faute d'appelant de production
 * (`ETAPE-10.md`) ; ce point d'entrée impose un ordre explicite (le plus récent en premier) plutôt
 * que d'en hériter un par accident.
 */
class UnlockAwareTechnicalEventLog
    @Inject
    constructor(
        private val unlockState: UnlockState,
        private val inMemory: InMemoryTechnicalEventLog,
        private val roomLog: Provider<RoomTechnicalEventLog>,
    ) : TechnicalEventLog {
        override fun log(
            type: TechnicalEventType,
            sessionId: String?,
            detailsJson: String?,
        ) {
            if (unlockState.isUserUnlocked) {
                roomLog.get().log(type, sessionId, detailsJson)
            } else {
                inMemory.log(type, sessionId, detailsJson)
            }
        }

        override suspend fun recent(): List<TechnicalEventEntry> {
            val fromRoom = if (unlockState.isUserUnlocked) roomLog.get().recent() else emptyList()
            return (inMemory.recent() + fromRoom)
                .sortedByDescending { it.occurredAtEpochMillis }
                .take(MAX_ENTRIES)
        }
    }
