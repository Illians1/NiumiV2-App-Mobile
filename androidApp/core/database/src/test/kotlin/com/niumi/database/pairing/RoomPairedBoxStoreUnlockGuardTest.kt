package com.niumi.database.pairing

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.NiumiDatabase
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.inject.Provider

/**
 * `ROOM_BEFORE_UNLOCK` (SPEC_ANDROID §7.3), écart documenté dans le KDoc de [RoomPairedBoxStore] :
 * [RoomPairedBoxStore.all] renvoie une liste vide avant déverrouillage sans jamais résoudre
 * [NiumiDatabase] (le diagnostic de réconciliation `LOCKED_BOOT` doit pouvoir s'exécuter) ;
 * [RoomPairedBoxStore.add], [RoomPairedBoxStore.rename] et [RoomPairedBoxStore.remove] gardent le
 * refus strict des autres dépôts Room, la base n'étant jamais résolue (Lot 8).
 */
class RoomPairedBoxStoreUnlockGuardTest {
    private object LockedState : UnlockState {
        override val isUserUnlocked: Boolean = false
    }

    private val neverCalledProvider =
        Provider<NiumiDatabase> { error("La base ne doit jamais être résolue avant déverrouillage.") }

    private val store = RoomPairedBoxStore(neverCalledProvider, LockedState)

    @Test
    fun `all returns an empty list before unlock without resolving the database`() =
        runBlocking {
            assertThat(store.all()).isEmpty()
        }

    @Test
    fun `add is refused before unlock`() {
        val credential = PairedBoxCredentialDto(1, "550e8400-e29b-41d4-a716-446655440000", "a".repeat(64))
        val exception =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { store.add(credential, "Cuisine") }
            }
        assertThat(exception).hasMessageThat().isEqualTo("ROOM_BEFORE_UNLOCK")
    }

    @Test
    fun `rename is refused before unlock`() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { store.rename("550e8400-e29b-41d4-a716-446655440000", "Cuisine") }
            }
        assertThat(exception).hasMessageThat().isEqualTo("ROOM_BEFORE_UNLOCK")
    }

    @Test
    fun `remove is refused before unlock`() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { store.remove("550e8400-e29b-41d4-a716-446655440000") }
            }
        assertThat(exception).hasMessageThat().isEqualTo("ROOM_BEFORE_UNLOCK")
    }
}
