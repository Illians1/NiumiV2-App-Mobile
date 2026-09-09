package com.niumi.database.logging

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.database.NiumiDatabase
import com.niumi.database.newInMemoryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §17 : 200 événements maximum, la plus ancienne supprimée au-delà.
 *
 * `log()` est fire-and-forget par contrat : le test injecte un vrai [CoroutineScope] et attend
 * explicitement la fin de ses enfants ([awaitWrites]) plutôt que d'utiliser le temps virtuel de
 * `runTest` — le `StandardTestDispatcher` de `runTest` met les `launch` en file sans les exécuter,
 * et une écriture Room suspend de toute façon sur des threads que le planificateur de test ne
 * contrôle pas.
 */
@RunWith(AndroidJUnit4::class)
class RoomTechnicalEventLogTest {
    private lateinit var database: NiumiDatabase
    private lateinit var scope: CoroutineScope

    @Before
    @Suppress("InjectDispatcher") // Le test est ici le fournisseur du scope de l'objet sous test.
    fun setUp() {
        database = newInMemoryDatabase()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() = database.close()

    private fun awaitWrites() =
        runBlocking {
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
        }

    /** Horodatage strictement croissant, comme une vraie horloge entre deux appels. */
    private fun newLog(startAtEpochMillis: Long = 1_700_000_000_000L): RoomTechnicalEventLog {
        var tick = startAtEpochMillis
        return RoomTechnicalEventLog(database.technicalEventDao(), scope, nowEpochMillis = { tick++ })
    }

    @Test
    fun keeps200MostRecentEntriesAndDropsTheOldest() {
        val log = newLog()

        repeat(201) { index -> log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "session-$index") }
        awaitWrites()

        val entries = runBlocking { log.recent() }
        assertThat(entries).hasSize(200)
        assertThat(entries.first().sessionId).isEqualTo("session-200")
        assertThat(entries.last().sessionId).isEqualTo("session-1")
    }

    @Test
    fun detailsJsonIsFilteredByType() {
        val log = newLog()

        log.log(TechnicalEventType.BLOCK_APPLIED, detailsJson = TechnicalEventDetails.packageName("com.example.app"))
        log.log(TechnicalEventType.ALARM_RECEIVED, detailsJson = TechnicalEventDetails.packageName("com.example.app"))
        awaitWrites()

        val entries = runBlocking { log.recent() }
        val blockApplied = entries.single { it.type == TechnicalEventType.BLOCK_APPLIED }
        val alarmReceived = entries.single { it.type == TechnicalEventType.ALARM_RECEIVED }
        assertThat(blockApplied.detailsJson).isEqualTo(TechnicalEventDetails.packageName("com.example.app"))
        assertThat(alarmReceived.detailsJson).isNull()
    }

    @Test
    fun anUnknownSessionIdIsAcceptedWithoutAForeignKeyViolation() {
        val log = newLog()

        log.log(TechnicalEventType.SESSION_FAILED, sessionId = "session-never-persisted")
        awaitWrites()

        assertThat(runBlocking { log.recent() }.single().sessionId).isEqualTo("session-never-persisted")
    }

    @Test
    fun writesKeepTheOrderOfTheCallsEvenThoughLoggingIsFireAndForget() {
        val log = newLog()

        repeat(50) { index -> log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "session-$index") }
        awaitWrites()

        val entries = runBlocking { log.recent() }
        assertThat(entries.map { it.sessionId })
            .containsExactlyElementsIn((49 downTo 0).map { "session-$it" })
            .inOrder()
    }
}
