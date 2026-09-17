package com.niumi.system.setup

import com.google.common.truth.Truth.assertThat
import com.niumi.database.BlockedPackage
import com.niumi.system.apps.DataStoreAppSelectionStore
import com.niumi.system.boot.fakes.FakeUnlockState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.inject.Provider

/**
 * Garde de déverrouillage des deux `DataStore` en stockage chiffré par les identifiants
 * (SPEC_ANDROID §7.3, étape 19).
 *
 * **Défaut mesuré sur appareil le 2026-09-14.** Depuis que `SystemEventsReceiver` réveille le
 * processus sur `LOCKED_BOOT_COMPLETED`, la réconciliation Direct Boot rejoue le diagnostic de §13,
 * qui lit ces deux dépôts. Le répertoire n'existe pas encore
 * (`Failed to ensure /data/user/0/… : mkdir failed: errno 126`), et l'instance de `DataStore` née
 * dans cette fenêtre **continue de servir un état vide après le déverrouillage**, pour toute la
 * durée de vie du processus : la sélection d'applications de l'utilisateur devenait invisible et
 * aucune nouvelle session ne pouvait être préparée jusqu'à ce que le processus soit recréé.
 *
 * Le `Provider<Context>` lève à dessein : le test prouve ainsi que le dépôt n'est **jamais résolu**
 * avant déverrouillage, et pas seulement que son résultat est ignoré — c'est la naissance de
 * l'instance qui fait le dégât.
 */
class DataStoreUnlockGuardTest {
    private val locked = FakeUnlockState(isUserUnlocked = false)

    @Test
    fun theAppSelectionIsEmptyBeforeUnlockWithoutResolvingTheDataStore() =
        runBlocking {
            val store = DataStoreAppSelectionStore(NO_CONTEXT, locked)

            assertThat(store.selection()).isEmpty()
            assertThat(store.selectedCount()).isEqualTo(0)
        }

    @Test
    fun writingTheAppSelectionIsRefusedBeforeUnlock() {
        val store = DataStoreAppSelectionStore(NO_CONTEXT, locked)

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { store.replace(listOf(BlockedPackage("com.example.app", "Exemple"))) }
            }
        assertThat(thrown).hasMessageThat().isEqualTo("DATASTORE_BEFORE_UNLOCK")
    }

    @Test
    fun setupPreferencesReadNeutralValuesBeforeUnlockWithoutResolvingTheDataStore() =
        runBlocking {
            val preferences = DataStoreSetupPreferences(NO_CONTEXT, locked)

            assertThat(preferences.isOnboardingAcknowledged()).isFalse()
            assertThat(preferences.isBatteryExemptionConfirmed()).isFalse()
            assertThat(preferences.lastWakeTimeIso()).isNull()
            assertThat(preferences.lastBlockingStartTimeIso()).isNull()
        }

    @Test
    fun writingSetupPreferencesIsRefusedBeforeUnlock() {
        val preferences = DataStoreSetupPreferences(NO_CONTEXT, locked)

        listOf<suspend () -> Unit>(
            { preferences.acknowledgeOnboarding() },
            { preferences.setBatteryExemptionConfirmed(confirmed = true) },
            { preferences.setLastWakeTimeIso("07:00") },
            { preferences.setLastBlockingStartTimeIso("22:30") },
            // Effacer la clé est une écriture comme une autre : la garde doit la refuser aussi,
            // sans quoi le chemin `null` du Lot 6 contournerait §7.3.
            { preferences.setLastBlockingStartTimeIso(null) },
        ).forEach { write ->
            val thrown = assertThrows(IllegalStateException::class.java) { runBlocking { write() } }
            assertThat(thrown).hasMessageThat().isEqualTo("DATASTORE_BEFORE_UNLOCK")
        }
    }

    private companion object {
        /**
         * Lève si on le résout : c'est ainsi que le test prouve que le `DataStore` n'est jamais
         * créé avant déverrouillage, et pas seulement que son résultat est ignoré. Même patron que
         * le `Provider<NiumiDatabase>` de `RoomSessionStoreUnlockGuardTest`.
         */
        val NO_CONTEXT =
            Provider<android.content.Context> {
                error("Le contexte ne doit jamais être résolu avant déverrouillage.")
            }
    }
}
