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
 * SPEC_ANDROID §7.2 et §11.1 (Lot 8) : `name` fait 1 à 30 caractères après `trim()`, et c'est le
 * dépôt qui tient cet invariant — point de vigilance 2 du lot, « refusé par `PairedBoxName.sanitize`
 * avant toute écriture ». La base n'est jamais résolue : le refus précède tout accès à Room.
 */
class RoomPairedBoxStoreNameGuardTest {
    private object UnlockedState : UnlockState {
        override val isUserUnlocked: Boolean = true
    }

    private val neverCalledProvider =
        Provider<NiumiDatabase> { error("La base ne doit jamais être résolue pour un nom refusé.") }

    private val store = RoomPairedBoxStore(neverCalledProvider, UnlockedState)

    private val credential = PairedBoxCredentialDto(1, "550e8400-e29b-41d4-a716-446655440000", "a".repeat(64))

    @Test
    fun `add with a blank name is refused before the database is resolved`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { store.add(credential, "   ") }
            }
        assertThat(exception).hasMessageThat().isEqualTo("PAIRED_BOX_NAME_INVALID")
    }

    @Test
    fun `rename with thirty-one characters is refused before the database is resolved`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { store.rename(credential.boxId, "x".repeat(31)) }
            }
        assertThat(exception).hasMessageThat().isEqualTo("PAIRED_BOX_NAME_INVALID")
    }
}
