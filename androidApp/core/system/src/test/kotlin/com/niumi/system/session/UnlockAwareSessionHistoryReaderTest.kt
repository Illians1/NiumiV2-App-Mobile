package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.SessionStoreUnreadableException
import com.niumi.database.directboot.DirectBootMapper
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.boot.fakes.InMemoryDirectBootStore
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Étape 46 : la session suivie par l'écran de réveil ou la feuille d'annulation se relit dans Room
 * après déverrouillage, dans la projection Direct Boot avant — et seulement si la projection porte
 * cette session (SPEC_ANDROID §7.3).
 */
class UnlockAwareSessionHistoryReaderTest {
    private val unlockState = FakeUnlockState(isUserUnlocked = true)
    private val directBootStore = InMemoryDirectBootStore()
    private val roomLookups = mutableListOf<String>()
    private val roomSnapshot = SessionDtoFixtures.snapshotInState(SessionStateDto.COMPLETED)

    private val reader =
        UnlockAwareSessionHistoryReader(
            historyStore = { id ->
                roomLookups += id
                roomSnapshot.takeIf { it.sessionId == id }
            },
            directBootStore = directBootStore,
            unlockState = unlockState,
        )

    private fun seedDirectBoot(sessionId: String) {
        directBootStore.seed(
            DirectBootMapper.projectionOf(
                snapshot = SessionDtoFixtures.snapshotInState(SessionStateDto.RELEASING, sessionId = sessionId),
                extras = SessionDtoFixtures.extras(),
                receipts = emptyList(),
                effects = emptyList(),
            ),
        )
    }

    @Test
    fun unlockedTheSessionIsReadFromRoom() =
        runTest {
            val found = reader.find(SessionDtoFixtures.SESSION_ID)

            assertThat(found?.state).isEqualTo(SessionStateDto.COMPLETED)
            assertThat(roomLookups).containsExactly(SessionDtoFixtures.SESSION_ID)
        }

    @Test
    fun lockedTheProjectionOfTheSameSessionIsReadAndRoomIsNeverTouched() =
        runTest {
            unlockState.isUserUnlocked = false
            seedDirectBoot(SessionDtoFixtures.SESSION_ID)

            val found = reader.find(SessionDtoFixtures.SESSION_ID)

            assertThat(found?.state).isEqualTo(SessionStateDto.RELEASING)
            assertThat(roomLookups).isEmpty()
        }

    @Test
    fun lockedAProjectionOfAnotherSessionIsNotAnAnswer() =
        runTest {
            unlockState.isUserUnlocked = false
            seedDirectBoot(SessionDtoFixtures.OTHER_SESSION_ID)

            assertThat(reader.find(SessionDtoFixtures.SESSION_ID)).isNull()
            assertThat(roomLookups).isEmpty()
        }

    /**
     * Relecture : une base Room illisible ne fait jamais planter l'écran qui relit (SPEC_ANDROID
     * §18) ; la session est alors introuvable, et chaque écran garde sa sortie de repli.
     */
    @Test
    fun anUnreadableDatabaseReadsAsNotFound() =
        runTest {
            val unreadable =
                UnlockAwareSessionHistoryReader(
                    historyStore = { throw SessionStoreUnreadableException("SQLITE_CORRUPT") },
                    directBootStore = directBootStore,
                    unlockState = unlockState,
                )

            assertThat(unreadable.find(SessionDtoFixtures.SESSION_ID)).isNull()
        }
}
