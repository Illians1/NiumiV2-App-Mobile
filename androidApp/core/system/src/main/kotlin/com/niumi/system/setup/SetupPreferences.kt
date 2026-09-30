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
 * Trois préférences de la mise en route, persistées hors Room (SPEC_ANDROID §4.5, §15) :
 *
 * - `onboardingAcknowledged` : l'utilisateur a lu les limites du produit avant sa première
 *   activation, notamment l'absence de tout secours logiciel pendant une session ;
 * - `lastWakeTimeIso` (étape 14) : dernière heure choisie sur l'écran de choix de l'heure, pour
 *   que le cadran s'ouvre dessus plutôt que sur 07:00 à chaque nouvelle préparation. `null` tant
 *   qu'aucune heure n'a jamais été confirmée ;
 * - `lastBlockingStartTimeIso` (étape 24, Lot 6) : dernière heure de début de blocage confirmée,
 *   mémorisée comme l'heure de réveil (SPEC_ANDROID §15, écran 5). `null` signifie « Maintenant »,
 *   qui reste le défaut tant qu'aucun début différé n'a été confirmé — d'où une écriture nulle qui
 *   **retire** la clé plutôt que d'y poser une chaîne vide.
 *
 * La confirmation de l'exemption d'énergie (`battery_exemption_confirmed`) a été retirée à
 * l'étape 25 : le diagnostic détecte désormais l'exemption (§13). La clé déjà écrite chez un
 * utilisateur reste dans le fichier sans être relue, ce qui est sans effet ; aucune migration.
 */
interface SetupPreferences {
    suspend fun isOnboardingAcknowledged(): Boolean

    suspend fun acknowledgeOnboarding()

    suspend fun lastWakeTimeIso(): String?

    suspend fun setLastWakeTimeIso(value: String)

    suspend fun lastBlockingStartTimeIso(): String?

    suspend fun setLastBlockingStartTimeIso(value: String?)
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

    override suspend fun lastWakeTimeIso(): String? = preferences()?.get(LAST_WAKE_TIME_ISO)

    override suspend fun setLastWakeTimeIso(value: String) = write(LAST_WAKE_TIME_ISO, value)

    override suspend fun lastBlockingStartTimeIso(): String? = preferences()?.get(LAST_BLOCKING_START_TIME_ISO)

    override suspend fun setLastBlockingStartTimeIso(value: String?) = write(LAST_BLOCKING_START_TIME_ISO, value)

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
     * Une valeur nulle **retire** la clé, ce dont le Lot 6 a besoin pour revenir à « Maintenant »
     * (§15). C'est une écriture comme une autre : la garde est la même, sinon ce chemin ferait
     * naître le `DataStore` avant déverrouillage — précisément ce que l'étape 19 interdit.
     */
    private suspend fun <T> write(
        key: Preferences.Key<T>,
        value: T?,
    ) {
        check(unlockState.isUserUnlocked) { DATASTORE_BEFORE_UNLOCK }
        contextProvider.get().setupDataStore.edit { preferences ->
            if (value == null) preferences.remove(key) else preferences[key] = value
        }
    }

    private companion object {
        const val DATASTORE_BEFORE_UNLOCK = "DATASTORE_BEFORE_UNLOCK"

        val ONBOARDING_ACKNOWLEDGED = booleanPreferencesKey("onboarding_acknowledged")
        val LAST_WAKE_TIME_ISO = stringPreferencesKey("last_wake_time_iso")
        val LAST_BLOCKING_START_TIME_ISO = stringPreferencesKey("last_blocking_start_time_iso")
    }
}
