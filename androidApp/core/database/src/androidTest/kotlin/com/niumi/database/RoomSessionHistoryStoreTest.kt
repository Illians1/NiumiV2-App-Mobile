package com.niumi.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Étape 46 : une session finie se relit par son identifiant après l'effacement du pointeur actif,
 * même quand une nouvelle session est déjà armée (écran 11, feuille d'annulation). Instrumenté :
 * Room refuse toute requête hors d'un appareil.
 */
@RunWith(AndroidJUnit4::class)
class RoomSessionHistoryStoreTest {
    private lateinit var database: NiumiDatabase
    private lateinit var sessionStore: RoomSessionStore
    private lateinit var historyStore: RoomSessionHistoryStore

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        sessionStore = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
        historyStore = RoomSessionHistoryStore(Provider { database }, AlwaysUnlockedState)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seed(
        sessionId: String,
        state: SessionStateDto,
    ) {
        sessionStore.commitDecision(
            StoredDecision(
                RoomTestFixtures.preparingSnapshot(sessionId).copy(state = state),
                RoomTestFixtures.receipt("event-$sessionId", sessionId),
                emptyList(),
                RoomTestFixtures.extras(),
            ),
        )
    }

    @Test
    fun aFinishedSessionIsFoundAfterTheNextSessionIsArmed() =
        runTest {
            seed(FINISHED_ID, SessionStateDto.COMPLETED)
            sessionStore.clearActivePointer(FINISHED_ID)
            seed(NEXT_ID, SessionStateDto.ARMED)

            val found = historyStore.find(FINISHED_ID)

            assertThat(found?.sessionId).isEqualTo(FINISHED_ID)
            assertThat(found?.state).isEqualTo(SessionStateDto.COMPLETED)
        }

    @Test
    fun anUnknownSessionIsNotFound() =
        runTest {
            seed(NEXT_ID, SessionStateDto.ARMED)

            assertThat(historyStore.find(FINISHED_ID)).isNull()
        }

    @Test
    fun theDatabaseIsNeverOpenedBeforeUnlock() {
        var opened = false
        val locked =
            RoomSessionHistoryStore(
                Provider {
                    opened = true
                    database
                },
                LockedState,
            )

        assertThrows(IllegalStateException::class.java) { runBlocking { locked.find(FINISHED_ID) } }
        assertThat(opened).isFalse()
    }

    private object LockedState : UnlockState {
        override val isUserUnlocked = false
    }

    private companion object {
        const val FINISHED_ID = "66666666-6666-6666-6666-666666666666"
        const val NEXT_ID = "77777777-7777-7777-7777-777777777777"
    }
}
