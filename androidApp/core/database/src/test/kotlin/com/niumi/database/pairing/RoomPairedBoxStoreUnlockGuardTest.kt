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
 * [RoomPairedBoxStore.current] renvoie `null` avant déverrouillage sans jamais résoudre
 * [NiumiDatabase] (le diagnostic de réconciliation `LOCKED_BOOT` doit pouvoir s'exécuter) ;
 * [RoomPairedBoxStore.replace] et [RoomPairedBoxStore.clear] gardent le refus strict des autres
 * dépôts Room, la base n'étant jamais résolue.
 */
class RoomPairedBoxStoreUnlockGuardTest {
    private object LockedState : UnlockState {
        override val isUserUnlocked: Boolean = false
    }

    private val neverCalledProvider =
        Provider<NiumiDatabase> { error("La base ne doit jamais être résolue avant déverrouillage.") }

    private val store = RoomPairedBoxStore(neverCalledProvider, LockedState)

    @Test
    fun `current returns null before unlock without resolving the database`() =
        runBlocking {
            assertThat(store.current()).isNull()
        }

    @Test
    fun `replace is refused before unlock`() {
        val credential = PairedBoxCredentialDto(1, "550e8400-e29b-41d4-a716-446655440000", "a".repeat(64))
        val exception =
            assertThrows(IllegalStateException::class.java) { runBlocking { store.replace(credential) } }
        assertThat(exception).hasMessageThat().isEqualTo("ROOM_BEFORE_UNLOCK")
    }

    @Test
    fun `clear is refused before unlock`() {
        val exception = assertThrows(IllegalStateException::class.java) { runBlocking { store.clear() } }
        assertThat(exception).hasMessageThat().isEqualTo("ROOM_BEFORE_UNLOCK")
    }
}
