package com.niumi.database.logging

import com.niumi.database.dao.TechnicalEventDao
import com.niumi.database.entity.TechnicalEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_ENTRIES = 200

/**
 * Implémentation Room de [TechnicalEventLog] (SPEC_ANDROID §7.2, §17). `log()` reste synchrone :
 * appelé depuis `onReceive`, `onStartCommand` et `onAccessibilityEvent`, tous sur le thread
 * principal, où Room interdit toute requête. L'écriture est postée sur [scope] (injecté ; un
 * `TestScope` en test rend l'écriture déterministe) et ses exceptions sont avalées — un journal ne
 * doit jamais faire planter le parcours qu'il observe, et `AlarmReceiver` (`directBootAware`) peut
 * appeler `log()` avant que Room ne soit accessible (avant `UserManager.isUserUnlocked`,
 * SPEC_ANDROID §7.3). Conséquence assumée : un événement journalisé dans cette fenêtre est perdu ;
 * c'est du diagnostic, jamais un chemin critique.
 *
 * Insertion et purge au-delà de [MAX_ENTRIES] dans la même transaction
 * ([TechnicalEventDao.insertAndPurge]), sérialisées par [writeMutex] : sans lui, deux `log()`
 * successifs lanceraient deux coroutines concurrentes dont l'ordre d'insertion — donc l'ordre des
 * `id` sur lequel repose la purge — ne suivrait pas l'ordre des appels. L'horodatage, lui, est
 * capturé au moment de l'appel, jamais au moment de l'écriture.
 */
class RoomTechnicalEventLog(
    private val technicalEventDao: TechnicalEventDao,
    private val scope: CoroutineScope,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : TechnicalEventLog {
    private val writeMutex = Mutex()

    override fun log(
        type: TechnicalEventType,
        sessionId: String?,
        detailsJson: String?,
    ) {
        val entity =
            TechnicalEventEntity(
                sessionId = sessionId,
                type = type.name,
                createdAtEpochMillis = nowEpochMillis(),
                detailsJson = TechnicalEventDetails.sanitize(type, detailsJson),
            )
        scope.launch {
            writeMutex.withLock {
                runCatching { technicalEventDao.insertAndPurge(entity, MAX_ENTRIES) }
            }
        }
    }

    override suspend fun recent(): List<TechnicalEventEntry> =
        technicalEventDao.mostRecent(MAX_ENTRIES).map { it.toEntry() }

    private fun TechnicalEventEntity.toEntry(): TechnicalEventEntry =
        TechnicalEventEntry(
            type = TechnicalEventType.valueOf(type),
            sessionId = sessionId,
            detailsJson = detailsJson,
            occurredAtEpochMillis = createdAtEpochMillis,
        )
}
