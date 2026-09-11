package com.niumi.database.pairing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.AlwaysUnlockedState
import com.niumi.database.NiumiDatabase
import com.niumi.database.newInMemoryDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * SPEC_ANDROID §11.1 : un seul boîtier associé, toute nouvelle association remplace l'ancienne.
 * `current()` avant déverrouillage renvoie `null` plutôt que de lever, à la différence du reste
 * de `RoomPairedBoxStore` — voir le KDoc de la classe.
 */
@RunWith(AndroidJUnit4::class)
class RoomPairedBoxStoreTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomPairedBoxStore

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomPairedBoxStore(Provider { database }, AlwaysUnlockedState)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun currentIsNullWhenNothingIsPaired() =
        runTest {
            assertThat(store.current()).isNull()
        }

    @Test
    fun replaceThenCurrentReturnsTheSameCredential() =
        runTest {
            val credential = PairedBoxCredentialDto(1, "550e8400-e29b-41d4-a716-446655440000", "a".repeat(64))

            store.replace(credential)

            assertThat(store.current()).isEqualTo(credential)
        }

    @Test
    fun replacingWithADifferentBoxIdLeavesExactlyOneBoxStored() =
        runTest {
            store.replace(PairedBoxCredentialDto(1, "550e8400-e29b-41d4-a716-446655440000", "a".repeat(64)))

            val secondCredential = PairedBoxCredentialDto(1, "00000000-0000-0000-0000-000000000000", "b".repeat(64))
            store.replace(secondCredential)

            assertThat(store.current()).isEqualTo(secondCredential)
            assertThat(database.pairedBoxDao().findById("550e8400-e29b-41d4-a716-446655440000")).isNull()
        }

    @Test
    fun clearRemovesTheStoredBox() =
        runTest {
            store.replace(PairedBoxCredentialDto(1, "550e8400-e29b-41d4-a716-446655440000", "a".repeat(64)))

            store.clear()

            assertThat(store.current()).isNull()
        }
}
