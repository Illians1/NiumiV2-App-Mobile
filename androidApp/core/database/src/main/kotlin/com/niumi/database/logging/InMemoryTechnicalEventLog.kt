package com.niumi.database.logging

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val MAX_ENTRIES = 200

/**
 * Implémentation provisoire de [TechnicalEventLog], en mémoire, remplacée par Room à
 * l'étape 9 sans changer l'interface. `nowEpochMillis` est injectable pour ne jamais dépendre
 * d'une vraie horloge en test. Construite via `LoggingModule` (pas de constructeur `@Inject` :
 * Dagger ne respecte pas les valeurs par défaut Kotlin des paramètres de constructeur).
 */
class InMemoryTechnicalEventLog(
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : TechnicalEventLog {
    private val lock = ReentrantLock()
    private val entries = ArrayDeque<TechnicalEventEntry>()

    override fun log(
        type: TechnicalEventType,
        sessionId: String?,
        packageName: String?,
    ) {
        val effectivePackageName = if (type == TechnicalEventType.BLOCK_APPLIED) packageName else null
        val entry =
            TechnicalEventEntry(
                type = type,
                sessionId = sessionId,
                packageName = effectivePackageName,
                occurredAtEpochMillis = nowEpochMillis(),
            )
        lock.withLock {
            entries.addLast(entry)
            while (entries.size > MAX_ENTRIES) entries.removeFirst()
        }
    }

    override fun recent(): List<TechnicalEventEntry> = lock.withLock { entries.toList() }
}
