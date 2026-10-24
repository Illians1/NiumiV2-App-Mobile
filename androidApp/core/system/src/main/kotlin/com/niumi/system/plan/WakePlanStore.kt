package com.niumi.system.plan

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Provider

/**
 * Stockage du planning (étape 43). Consommé par le `WakePlanScheduler` de l'étape 44, qui arme,
 * désarme et ré-arme les sessions, et par l'accueil (étape 45) : le planning est la source de la carte
 * et du cadran, la session celle du verrouillage et du thème (décision 4 du Lot 13).
 */
interface WakePlanStore {
    /** [WakePlan.DEFAULT] avant déverrouillage ou si rien n'a été écrit. */
    suspend fun read(): WakePlan

    /** Lève `IllegalArgumentException("WAKE_PLAN_INVALID")` avant toute écriture pour un planning invalide. */
    suspend fun write(plan: WakePlan)

    /** Réémis à chaque écriture. Une seule émission, [WakePlan.DEFAULT], avant déverrouillage. */
    val plan: Flow<WakePlan>
}

private val Context.wakePlanDataStore: DataStore<Preferences> by preferencesDataStore(name = "niumi_wake_plan")

/**
 * Même garde de déverrouillage que `DataStoreSetupPreferences` (SPEC_ANDROID §7.3) : [contextProvider]
 * n'est jamais résolu avant déverrouillage, lecture neutre ([WakePlan.DEFAULT]), écriture refusée.
 *
 * **La lecture neutre n'est pas une extinction.** Avant déverrouillage, l'interrupteur se lit éteint
 * parce que le planning est illisible, pas parce que l'utilisateur l'a éteint : aucun consommateur ne
 * doit en déduire un désarmement. Le ré-armement n'a lieu qu'après `USER_UNLOCKED` (point de
 * vigilance 6 du plan de refonte, étape 44).
 */
class DataStoreWakePlanStore(
    private val contextProvider: Provider<Context>,
    private val unlockState: UnlockState,
) : WakePlanStore {
    override suspend fun read(): WakePlan {
        if (!unlockState.isUserUnlocked) return WakePlan.DEFAULT
        return decoded().first()
    }

    override suspend fun write(plan: WakePlan) {
        check(unlockState.isUserUnlocked) { DATASTORE_BEFORE_UNLOCK }
        plan.requireValid()
        contextProvider.get().wakePlanDataStore.edit { preferences ->
            preferences[ENABLED] = plan.enabled
            preferences[DAYS] = encodeIsoDays(plan.enabledIsoDays)
            preferences[WAKE_TIME] = plan.wakeTimeIso
            preferences[BEDTIME] = plan.bedtimeIso
        }
    }

    override val plan: Flow<WakePlan> =
        flow {
            if (!unlockState.isUserUnlocked) emit(WakePlan.DEFAULT) else emitAll(decoded())
        }

    private fun decoded(): Flow<WakePlan> =
        contextProvider
            .get()
            .wakePlanDataStore.data
            .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
            .map { preferences ->
                decodeWakePlan(preferences[ENABLED], preferences[DAYS], preferences[WAKE_TIME], preferences[BEDTIME])
            }

    private companion object {
        const val DATASTORE_BEFORE_UNLOCK = "DATASTORE_BEFORE_UNLOCK"

        val ENABLED = booleanPreferencesKey("enabled")
        val DAYS = stringPreferencesKey("days")
        val WAKE_TIME = stringPreferencesKey("wake_time")
        val BEDTIME = stringPreferencesKey("bedtime")
    }
}
