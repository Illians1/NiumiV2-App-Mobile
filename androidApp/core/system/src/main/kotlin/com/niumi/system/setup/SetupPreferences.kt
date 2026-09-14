package com.niumi.system.setup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Provider

/**
 * Deux accusés de réception de la mise en route, persistés hors Room (SPEC_ANDROID §4.5, §13) :
 *
 * - `onboardingAcknowledged` : l'utilisateur a lu les limites du produit avant sa première
 *   activation, notamment l'absence de tout secours logiciel pendant une session ;
 * - `batteryExemptionConfirmed` : l'utilisateur confirme avoir levé les restrictions d'énergie.
 *   §13 exige cette confirmation parce que la détection système est partielle : sur HyperOS,
 *   `isIgnoringBatteryOptimizations()` reste `false` après correction du réglage OEM ;
 * - `lastWakeTimeIso` (étape 14) : dernière heure choisie sur l'écran de choix de l'heure, pour
 *   que le cadran s'ouvre dessus plutôt que sur 07:00 à chaque nouvelle préparation. `null` tant
 *   qu'aucune heure n'a jamais été confirmée.
 */
interface SetupPreferences {
    suspend fun isOnboardingAcknowledged(): Boolean

    suspend fun acknowledgeOnboarding()

    suspend fun isBatteryExemptionConfirmed(): Boolean

    suspend fun setBatteryExemptionConfirmed(confirmed: Boolean)

    suspend fun lastWakeTimeIso(): String?

    suspend fun setLastWakeTimeIso(value: String)
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

    override suspend fun isBatteryExemptionConfirmed(): Boolean = read(BATTERY_EXEMPTION_CONFIRMED)

    override suspend fun setBatteryExemptionConfirmed(confirmed: Boolean) =
        write(BATTERY_EXEMPTION_CONFIRMED, confirmed)

    override suspend fun lastWakeTimeIso(): String? = preferences()?.get(LAST_WAKE_TIME_ISO)

    override suspend fun setLastWakeTimeIso(value: String) = write(LAST_WAKE_TIME_ISO, value)

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

    private suspend fun <T> write(
        key: Preferences.Key<T>,
        value: T,
    ) {
        check(unlockState.isUserUnlocked) { DATASTORE_BEFORE_UNLOCK }
        contextProvider.get().setupDataStore.edit { preferences -> preferences[key] = value }
    }

    private companion object {
        const val DATASTORE_BEFORE_UNLOCK = "DATASTORE_BEFORE_UNLOCK"

        val ONBOARDING_ACKNOWLEDGED = booleanPreferencesKey("onboarding_acknowledged")
        val BATTERY_EXEMPTION_CONFIRMED = booleanPreferencesKey("battery_exemption_confirmed")
        val LAST_WAKE_TIME_ISO = stringPreferencesKey("last_wake_time_iso")
    }
}
