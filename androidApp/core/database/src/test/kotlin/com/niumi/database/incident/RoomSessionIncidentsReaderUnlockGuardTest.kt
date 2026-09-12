package com.niumi.database.incident

import com.google.common.truth.Truth.assertThat
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider

/**
 * Avant déverrouillage, Room ne doit pas même être construite (SPEC_ANDROID §7.3) : le [Provider]
 * piégé le prouve. La lecture renvoie une liste vide plutôt qu'une erreur — un incident manquant
 * ne doit jamais empêcher d'afficher le reste de la session. Même patron que
 * `RoomSessionStoreUnlockGuardTest`.
 */
class RoomSessionIncidentsReaderUnlockGuardTest {
    @Test
    fun beforeUnlockTheReaderIsEmptyAndNeverOpensTheDatabase() =
        runTest {
            val reader =
                RoomSessionIncidentsReader(
                    unlockState = LockedState,
                    databaseProvider = Provider { error("ROOM_OPENED_BEFORE_UNLOCK") },
                )

            assertThat(reader.incidents("11111111-1111-1111-1111-111111111111")).isEmpty()
        }

    private object LockedState : UnlockState {
        override val isUserUnlocked: Boolean = false
    }
}
