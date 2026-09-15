package com.niumi.database.logging

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Implémentation en mémoire de [TechnicalEventLog], conservée après l'introduction de
 * `RoomTechnicalEventLog` (étape 9) comme repli avant déverrouillage (`UserManager
 * .isUserUnlocked == false`, SPEC_ANDROID §7.3 : Room y est inaccessible) et comme double de
 * test. `nowEpochMillis` est injectable pour ne jamais dépendre d'une vraie horloge en test.
 * Construite via `LoggingModule` (pas de constructeur `@Inject` : Dagger ne respecte pas les
 * valeurs par défaut Kotlin des paramètres de constructeur).
 */
class InMemoryTechnicalEventLog(
    private val deviceContext: DeviceContext,
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
                deviceModel = deviceContext.deviceModel,
                androidVersion = deviceContext.androidVersion,
                appVersion = deviceContext.appVersion,
            )
        lock.withLock {
            entries.addLast(entry)
            while (entries.size > MAX_TECHNICAL_EVENTS) entries.removeFirst()
        }
    }

    override suspend fun recent(): List<TechnicalEventEntry> = lock.withLock { entries.toList() }

    /**
     * Vidange atomique pour le versement dans Room au déverrouillage (§9.3, §17 ; étape 20) :
     * jamais rejouée deux fois, sans quoi un second appel dupliquerait ce que le premier a déjà
     * versé.
     */
    fun drain(): List<TechnicalEventEntry> =
        lock.withLock {
            val drained = entries.toList()
            entries.clear()
            drained
        }
}
