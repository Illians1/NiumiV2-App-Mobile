package com.niumi.system.setup

import com.google.common.truth.Truth.assertThat
import com.niumi.database.BlockedPackage
import com.niumi.system.apps.DataStoreAppSelectionStore
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.DataStoreAlarmSoundPreferences
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.VolumeRampDurations
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.insight.DataStoreTimeToGetUpPreferences
import com.niumi.system.plan.DataStoreWakePlanStore
import com.niumi.system.plan.WakePlan
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.inject.Provider

/**
 * Garde de déverrouillage des `DataStore` en stockage chiffré par les identifiants
 * (SPEC_ANDROID §7.3, étape 19 ; planning et durée déclarée depuis l'étape 43).
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
            assertThat(preferences.isSetupCompleted()).isFalse()
            assertThat(preferences.isNotificationPermissionRequested()).isFalse()
        }

    @Test
    fun writingSetupPreferencesIsRefusedBeforeUnlock() {
        val preferences = DataStoreSetupPreferences(NO_CONTEXT, locked)

        listOf<suspend () -> Unit>(
            { preferences.acknowledgeOnboarding() },
            { preferences.markSetupCompleted() },
            { preferences.markNotificationPermissionRequested() },
        ).forEach { write ->
            val thrown = assertThrows(IllegalStateException::class.java) { runBlocking { write() } }
            assertThat(thrown).hasMessageThat().isEqualTo("DATASTORE_BEFORE_UNLOCK")
        }
    }

    @Test
    fun alarmSoundPreferencesReadTheDefaultsBeforeUnlockWithoutResolvingTheDataStore() =
        runBlocking {
            val preferences = DataStoreAlarmSoundPreferences(NO_CONTEXT, locked)

            assertThat(preferences.read())
                .isEqualTo(AlarmSoundSettings(NiumiRingtones.DEFAULT_KEY, VolumeRampDurations.DEFAULT_SECONDS))
        }

    @Test
    fun writingAlarmSoundPreferencesIsRefusedBeforeUnlock() {
        val preferences = DataStoreAlarmSoundPreferences(NO_CONTEXT, locked)

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { preferences.write(AlarmSoundSettings("niumi_oiseaux", 60)) }
            }
        assertThat(thrown).hasMessageThat().isEqualTo("DATASTORE_BEFORE_UNLOCK")
    }

    /**
     * Étape 43 : avant déverrouillage, le planning se lit comme le défaut, interrupteur éteint. Ce
     * n'est pas une extinction choisie : l'étape 44 ne ré-arme jamais avant `USER_UNLOCKED`.
     */
    @Test
    fun theWakePlanReadsTheDefaultBeforeUnlockWithoutResolvingTheDataStore() {
        val store = DataStoreWakePlanStore(NO_CONTEXT, locked)

        val read = runBlocking { store.read() }
        val emitted = runBlocking { store.plan.toList() }

        assertThat(read).isEqualTo(WakePlan.DEFAULT)
        assertThat(emitted).containsExactly(WakePlan.DEFAULT)
    }

    @Test
    fun writingTheWakePlanIsRefusedBeforeUnlock() {
        val store = DataStoreWakePlanStore(NO_CONTEXT, locked)

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { store.write(WakePlan.DEFAULT.copy(enabled = true)) }
            }
        assertThat(thrown).hasMessageThat().isEqualTo("DATASTORE_BEFORE_UNLOCK")
    }

    @Test
    fun theDeclaredDurationIsUnknownBeforeUnlockWithoutResolvingTheDataStore() =
        runBlocking {
            val preferences = DataStoreTimeToGetUpPreferences(NO_CONTEXT, locked)

            assertThat(preferences.declaredMinutes()).isNull()
        }

    @Test
    fun writingTheDeclaredDurationIsRefusedBeforeUnlock() {
        val preferences = DataStoreTimeToGetUpPreferences(NO_CONTEXT, locked)

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { preferences.setDeclaredMinutes(15) }
            }
        assertThat(thrown).hasMessageThat().isEqualTo("DATASTORE_BEFORE_UNLOCK")
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
