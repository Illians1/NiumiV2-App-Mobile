package com.niumi.system.setup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

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

class DataStoreSetupPreferences(
    private val context: Context,
) : SetupPreferences {
    override suspend fun isOnboardingAcknowledged(): Boolean = read(ONBOARDING_ACKNOWLEDGED)

    override suspend fun acknowledgeOnboarding() = write(ONBOARDING_ACKNOWLEDGED, value = true)

    override suspend fun isBatteryExemptionConfirmed(): Boolean = read(BATTERY_EXEMPTION_CONFIRMED)

    override suspend fun setBatteryExemptionConfirmed(confirmed: Boolean) =
        write(BATTERY_EXEMPTION_CONFIRMED, confirmed)

    override suspend fun lastWakeTimeIso(): String? = preferences()[LAST_WAKE_TIME_ISO]

    override suspend fun setLastWakeTimeIso(value: String) = write(LAST_WAKE_TIME_ISO, value)

    private suspend fun read(key: Preferences.Key<Boolean>): Boolean = preferences()[key] ?: false

    private suspend fun preferences(): Preferences =
        context.setupDataStore.data
            .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
            .first()

    private suspend fun <T> write(
        key: Preferences.Key<T>,
        value: T,
    ) {
        context.setupDataStore.edit { preferences -> preferences[key] = value }
    }

    private companion object {
        val ONBOARDING_ACKNOWLEDGED = booleanPreferencesKey("onboarding_acknowledged")
        val BATTERY_EXEMPTION_CONFIRMED = booleanPreferencesKey("battery_exemption_confirmed")
        val LAST_WAKE_TIME_ISO = stringPreferencesKey("last_wake_time_iso")
    }
}
