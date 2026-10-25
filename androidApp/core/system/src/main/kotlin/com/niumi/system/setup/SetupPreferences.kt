package com.niumi.system.setup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Provider

/**
 * Trois préférences de la mise en route, persistées hors Room (SPEC_ANDROID §4.5, §15) :
 *
 * - `onboardingAcknowledged` : l'utilisateur a lu l'écran 1 · Présentation jusqu'à « Configurer
 *   Niumi » (étape 35 de la refonte). La clé `onboarding_acknowledged` date de l'ancien onboarding
 *   des limites et n'est pas renommée : la renommer ferait revenir la présentation chez ceux qui
 *   l'avaient déjà acquitté ;
 * - `setupCompleted` (étape 45 de la refonte) : la mise en route est allée jusqu'à « Continuer » de
 *   l'écran 4 ; l'accueil s'ouvre alors directement (`design-plan/SPECIFICATION.md` §3). Une
 *   installation antérieure à l'étape 45 n'a pas la clé : `SetupCompletion` la déduit des Niumi Spots
 *   et des applications enregistrés ;
 * - `notificationPermissionRequested` (étape 36 de la refonte) : la fenêtre système des
 *   notifications a été proposée au moins une fois. Android ne dit pas si elle peut encore
 *   s'ouvrir ; ce souvenir, joint à `shouldShowRequestPermissionRationale`, décide entre la fenêtre
 *   et les réglages de notification avant de toucher « Autoriser » (SPEC_ANDROID §13).
 *
 * La confirmation de l'exemption d'énergie (`battery_exemption_confirmed`) a été retirée à
 * l'étape 25 : le diagnostic détecte désormais l'exemption (§13). Les dernières heures de l'ancien
 * écran 5 (`last_wake_time_iso`, `last_blocking_start_time_iso`) l'ont été à l'étape 45 : le planning
 * les remplace. Les clés déjà écrites restent dans le fichier sans être relues, ce qui est sans
 * effet ; aucune migration.
 */
interface SetupPreferences {
    suspend fun isOnboardingAcknowledged(): Boolean

    suspend fun acknowledgeOnboarding()

    suspend fun isSetupCompleted(): Boolean

    suspend fun markSetupCompleted()

    suspend fun isNotificationPermissionRequested(): Boolean

    suspend fun markNotificationPermissionRequested()
}

private val Context.setupDataStore: DataStore<Preferences> by preferencesDataStore(name = "niumi_setup")

/**
 * **Garde de déverrouillage (étape 19).** Ce `DataStore` vit dans le stockage chiffré par les
 * identifiants : avant le premier déverrouillage, son répertoire n'existe pas
 * (`mkdir failed: errno 126`). Y toucher n'est pas seulement inutile — c'est **destructeur pour la
 * durée de vie du processus** : l'instance créée dans cette fenêtre continue de servir un état vide
 * une fois l'appareil déverrouillé. Mesuré sur appareil le 2026-09-14 (étape 19), où un processus né
 * en Direct Boot ne voyait plus ni la sélection d'applications ni la confirmation d'énergie, rendant
 * impossible la préparation d'une session jusqu'à ce que le processus soit recréé.
 *
 * La garde n'accède donc **jamais** à [setupDataStore] avant déverrouillage — elle ne se contente pas
 * d'ignorer le résultat, elle empêche l'instance de naître : [contextProvider] n'est **jamais
 * résolu** avant déverrouillage, même patron que le `Provider<NiumiDatabase>` de
 * `RoomSessionStore`. Lectures neutres, écritures refusées :
 * mêmes conventions que `RoomSessionStore` (`ROOM_BEFORE_UNLOCK`) et `RoomPairedBoxStore`
 * (SPEC_ANDROID §7.3). Aucune écriture n'a lieu avant déverrouillage en pratique : ces trois
 * préférences ne changent que depuis l'interface.
 */
class DataStoreSetupPreferences(
    private val contextProvider: Provider<Context>,
    private val unlockState: UnlockState,
) : SetupPreferences {
    override suspend fun isOnboardingAcknowledged(): Boolean = read(ONBOARDING_ACKNOWLEDGED)

    override suspend fun acknowledgeOnboarding() = write(ONBOARDING_ACKNOWLEDGED, value = true)

    override suspend fun isSetupCompleted(): Boolean = read(SETUP_COMPLETED)

    override suspend fun markSetupCompleted() = write(SETUP_COMPLETED, value = true)

    override suspend fun isNotificationPermissionRequested(): Boolean = read(NOTIFICATION_PERMISSION_REQUESTED)

    override suspend fun markNotificationPermissionRequested() = write(NOTIFICATION_PERMISSION_REQUESTED, value = true)

    private suspend fun read(key: Preferences.Key<Boolean>): Boolean = preferences()?.get(key) ?: false

    /** `null` avant déverrouillage : le `DataStore` n'est même pas résolu. */
    private suspend fun preferences(): Preferences? {
        if (!unlockState.isUserUnlocked) return null
        return contextProvider
            .get()
            .setupDataStore.data
            .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
            .first()
    }

    /**
     * Même garde que les lectures : sans elle, ce chemin ferait naître le `DataStore` avant
     * déverrouillage (étape 19).
     */
    private suspend fun write(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ) {
        check(unlockState.isUserUnlocked) { DATASTORE_BEFORE_UNLOCK }
        contextProvider.get().setupDataStore.edit { preferences -> preferences[key] = value }
    }

    private companion object {
        const val DATASTORE_BEFORE_UNLOCK = "DATASTORE_BEFORE_UNLOCK"

        val ONBOARDING_ACKNOWLEDGED = booleanPreferencesKey("onboarding_acknowledged")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
