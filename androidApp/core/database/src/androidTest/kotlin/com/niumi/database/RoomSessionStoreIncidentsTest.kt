package com.niumi.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionIncidentDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * `SessionStore.recordIncident` (étape 11) : `SessionIncidentEntity` et `IncidentDao` existent
 * depuis l'étape 9 sans écrivain (`RECORD_INCIDENT` n'avait pas encore d'exécuteur). Passe par
 * `SessionStore` pour hériter de la garde `ROOM_BEFORE_UNLOCK`, comme tout accès Room.
 */
@RunWith(AndroidJUnit4::class)
class RoomSessionStoreIncidentsTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomSessionStore
    private val sessionId = "66666666-6666-6666-6666-666666666666"

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seedSession() {
        store.commitDecision(
            StoredDecision(
                RoomTestFixtures.preparingSnapshot(sessionId),
                RoomTestFixtures.receipt("seed-event", sessionId),
                emptyList(),
                RoomTestFixtures.extras(),
            ),
        )
    }

    @Test
    fun recordIncidentIsReadableThroughTheDaoAfterCommit() =
        runTest {
            seedSession()
            val incident =
                SessionIncidentDto(
                    code = "ALARM_PERMISSION_REVOKED",
                    severity = IncidentSeverityDto.CRITICAL,
                    occurredAtEpochMillis = 1_700_000_001_000L,
                    platform = PlatformDto.ANDROID,
                )

            store.recordIncident(sessionId, incident)

            val entities = database.incidentDao().forSession(sessionId)
            assertThat(entities).hasSize(1)
            assertThat(entities.single().code).isEqualTo("ALARM_PERMISSION_REVOKED")
            assertThat(entities.single().severity).isEqualTo(IncidentSeverityDto.CRITICAL)
        }

    @Test
    fun recordIncidentTwiceKeepsBothEntries() =
        runTest {
            seedSession()
            val first =
                SessionIncidentDto("TIME_CHANGED", IncidentSeverityDto.WARNING, 1_700_000_001_000L, PlatformDto.ANDROID)
            val second =
                SessionIncidentDto(
                    "MISSED_TRIGGER_WINDOW",
                    IncidentSeverityDto.DEGRADED,
                    1_700_000_002_000L,
                    PlatformDto.ANDROID,
                )

            store.recordIncident(sessionId, first)
            store.recordIncident(sessionId, second)

            assertThat(database.incidentDao().forSession(sessionId)).hasSize(2)
        }
}
