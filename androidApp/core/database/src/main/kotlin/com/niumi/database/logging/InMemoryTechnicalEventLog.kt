package com.niumi.database.logging

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val MAX_ENTRIES = 200

/**
 * Implémentation en mémoire de [TechnicalEventLog], conservée après l'introduction de
 * `RoomTechnicalEventLog` (étape 9) comme repli avant déverrouillage (`UserManager
 * .isUserUnlocked == false`, SPEC_ANDROID §7.3 : Room y est inaccessible) et comme double de
 * test. `nowEpochMillis` est injectable pour ne jamais dépendre d'une vraie horloge en test.
 * Construite via `LoggingModule` (pas de constructeur `@Inject` : Dagger ne respecte pas les
 * valeurs par défaut Kotlin des paramètres de constructeur).
 */
class InMemoryTechnicalEventLog(
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : TechnicalEventLog {
    private val lock = ReentrantLock()
    private val entries = ArrayDeque<TechnicalEventEntry>()

    override fun log(
        type: TechnicalEventType,
        sessionId: String?,
        detailsJson: String?,
    ) {
        val entry =
            TechnicalEventEntry(
                type = type,
                sessionId = sessionId,
                detailsJson = TechnicalEventDetails.sanitize(type, detailsJson),
                occurredAtEpochMillis = nowEpochMillis(),
            )
        lock.withLock {
            entries.addLast(entry)
            while (entries.size > MAX_ENTRIES) entries.removeFirst()
        }
    }

    override suspend fun recent(): List<TechnicalEventEntry> = lock.withLock { entries.toList() }
}
