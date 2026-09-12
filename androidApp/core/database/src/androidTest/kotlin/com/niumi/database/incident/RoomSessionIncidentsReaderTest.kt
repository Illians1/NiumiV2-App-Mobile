package com.niumi.database.incident

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.AlwaysUnlockedState
import com.niumi.database.NiumiDatabase
import com.niumi.database.RoomSessionStore
import com.niumi.database.RoomTestFixtures
import com.niumi.database.StoredDecision
import com.niumi.database.newInMemoryDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Premier lecteur d'`IncidentDao` (étape 15) : l'écran de session active doit présenter les
 * incidents `CRITICAL` explicitement (SPEC_CORE_KMP §7.3). Vérifie l'aller-retour complet avec
 * l'écrivain existant, `RoomSessionStore.recordIncident`.
 */
@RunWith(AndroidJUnit4::class)
class RoomSessionIncidentsReaderTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomSessionStore
    private lateinit var reader: RoomSessionIncidentsReader
    private val sessionId = "66666666-6666-6666-6666-666666666666"

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
        reader = RoomSessionIncidentsReader(AlwaysUnlockedState, Provider { database })
    }

    @After
    fun tearDown() = database.close()

    private fun incident(
        code: String,
        severity: IncidentSeverityDto,
        occurredAtEpochMillis: Long,
    ) = SessionIncidentDto(code, severity, occurredAtEpochMillis, PlatformDto.ANDROID)

    private suspend fun seedSession() {
        store.commitDecision(
            StoredDecision(
                snapshot = RoomTestFixtures.preparingSnapshot(sessionId),
                receipt = RoomTestFixtures.receipt("seed-event", sessionId),
                effects = emptyList(),
                androidExtras = RoomTestFixtures.extras(),
            ),
        )
    }

    @Test
    fun aSessionWithoutAnyIncidentReadsAsAnEmptyList() =
        runTest {
            seedSession()

            assertThat(reader.incidents(sessionId)).isEmpty()
        }

    @Test
    fun everyRecordedIncidentIsReadBackWithItsCodeSeverityAndInstant() =
        runTest {
            seedSession()
            val blocking = incident("BLOCKING_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL, 1_000L)
            val timeChanged = incident("TIME_CHANGED", IncidentSeverityDto.WARNING, 2_000L)
            store.recordIncident(sessionId, blocking)
            store.recordIncident(sessionId, timeChanged)

            assertThat(reader.incidents(sessionId)).containsExactly(blocking, timeChanged).inOrder()
        }

    @Test
    fun anUnknownSessionReadsAsAnEmptyList() =
        runTest {
            seedSession()
            store.recordIncident(sessionId, incident("TIME_CHANGED", IncidentSeverityDto.WARNING, 1_000L))

            assertThat(reader.incidents("00000000-0000-0000-0000-000000000000")).isEmpty()
        }
}
