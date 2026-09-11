package com.niumi.database

import com.google.common.truth.Truth.assertThat
import com.niumi.database.directboot.UnlockState
import com.niumi.database.mapping.SessionSnapshotDtoFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.inject.Provider

/**
 * `ROOM_BEFORE_UNLOCK` (SPEC_ANDROID §7.3) : verrouillé, chaque méthode de [SessionStore] doit
 * refuser sans jamais résoudre [NiumiDatabase] — pas seulement s'abstenir de l'utiliser. Le
 * [Provider] lève s'il est sollicité, ce qui prouve l'ordre (garde avant résolution).
 */
class RoomSessionStoreUnlockGuardTest {
    private object LockedState : UnlockState {
        override val isUserUnlocked: Boolean = false
    }

    private val neverCalledProvider =
        Provider<NiumiDatabase> { error("La base ne doit jamais être résolue avant déverrouillage.") }

    private val store = RoomSessionStore(neverCalledProvider, LockedState)

    private fun assertRefused(block: suspend () -> Unit) {
        val exception = assertThrows(IllegalStateException::class.java) { runBlocking { block() } }
        assertThat(exception).hasMessageThat().isEqualTo("ROOM_BEFORE_UNLOCK")
    }

    @Test
    fun `activeSession is refused before unlock`() = assertRefused { store.activeSession() }

    @Test
    fun `commitDecision is refused before unlock`() {
        val snapshot = SessionSnapshotDtoFixtures.preparingSnapshot()
        val decision =
            StoredDecision(
                snapshot = snapshot,
                receipt =
                    EventReceipt(
                        eventId = "e1",
                        sessionId = snapshot.sessionId,
                        payloadSha256Hex = "a".repeat(64),
                        appliedRevision = 1,
                        receivedAtEpochMillis = 1L,
                    ),
                effects = emptyList(),
                androidExtras = SessionSnapshotDtoFixtures.extras(),
            )
        assertRefused { store.commitDecision(decision) }
    }

    @Test
    fun `findReceipt is refused before unlock`() = assertRefused { store.findReceipt("e1") }

    @Test
    fun `pendingEffects is refused before unlock`() = assertRefused { store.pendingEffects("s1") }

    @Test
    fun `markEffect is refused before unlock`() =
        assertRefused {
            store.markEffect("effect-1", EffectStatus.SUCCEEDED, null)
        }

    @Test
    fun `clearActivePointer is refused before unlock`() = assertRefused { store.clearActivePointer("s1") }
}
