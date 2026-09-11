package com.niumi.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * `SessionStore.receipts` (étape 11) : nécessaire au miroir Direct Boot, qui projette
 * `eventReceipts` (SPEC_ANDROID §7.3) alors que `RoomSessionStore` ne savait relire qu'un reçu à
 * la fois via `findReceipt`.
 */
@RunWith(AndroidJUnit4::class)
class RoomSessionStoreReceiptsTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomSessionStore
    private val sessionId = "77777777-7777-7777-7777-777777777777"

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun commit(
        eventId: String,
        revision: Long,
    ) {
        store.commitDecision(
            StoredDecision(
                RoomTestFixtures.preparingSnapshot(sessionId, revision = revision),
                RoomTestFixtures.receipt(eventId, sessionId, appliedRevision = revision),
                emptyList(),
                RoomTestFixtures.extras(),
            ),
        )
    }

    @Test
    fun receiptsReturnsEveryReceiptCommittedForTheSession() =
        runTest {
            commit(eventId = "event-1", revision = 1)
            commit(eventId = "event-2", revision = 2)

            val receipts = store.receipts(sessionId)

            assertThat(receipts.map { it.eventId }).containsExactly("event-1", "event-2")
        }

    @Test
    fun receiptsForAnUnknownSessionIsEmpty() =
        runTest {
            assertThat(store.receipts("unknown-session")).isEmpty()
        }
}
