package com.niumi.system.insight

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Provider

/**
 * Les onze durées de la question « Combien de temps te faut-il pour sortir du lit ? »
 * (`design-plan/SPECIFICATION.md` §7) ; « 1 heure et + » compte pour 60 (D3).
 */
object DeclaredMinutes {
    val ALLOWED: List<Int> = listOf(1, 2, 3, 4, 5, 10, 15, 20, 30, 45, 60)
}

/**
 * Durée déclarée pour sortir du lit (étape 43), comparée au temps réel par `TimeToGetUpCalculator`
 * (rubrique « Temps gagné », étape 50). Aucune valeur présélectionnée n'est décidée (D4) : `null`
 * signifie « rien de déclaré », et le temps gagné ne se calcule pas.
 */
interface TimeToGetUpPreferences {
    /**
     * `null` avant déverrouillage, si rien n'a été déclaré, ou pour une valeur stockée hors de
     * [DeclaredMinutes.ALLOWED].
     */
    suspend fun declaredMinutes(): Int?

    /**
     * Lève `IllegalArgumentException("DECLARED_MINUTES_INVALID")` hors de [DeclaredMinutes.ALLOWED],
     * avant toute écriture.
     */
    suspend fun setDeclaredMinutes(value: Int)
}

/** Décodage pur, testable en JVM : une valeur hors liste n'est jamais propagée. */
internal fun decodeDeclaredMinutes(raw: Int?): Int? = raw?.takeIf { it in DeclaredMinutes.ALLOWED }

private val Context.timeToGetUpDataStore: DataStore<Preferences> by preferencesDataStore(name = "niumi_time_to_get_up")

/** Même garde de déverrouillage que `DataStoreSetupPreferences` (SPEC_ANDROID §7.3). */
class DataStoreTimeToGetUpPreferences(
    private val contextProvider: Provider<Context>,
    private val unlockState: UnlockState,
) : TimeToGetUpPreferences {
    override suspend fun declaredMinutes(): Int? {
        if (!unlockState.isUserUnlocked) return null
        val preferences =
            contextProvider
                .get()
                .timeToGetUpDataStore.data
                .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
                .first()
        return decodeDeclaredMinutes(preferences[DECLARED_MINUTES])
    }

    override suspend fun setDeclaredMinutes(value: Int) {
        check(unlockState.isUserUnlocked) { DATASTORE_BEFORE_UNLOCK }
        require(value in DeclaredMinutes.ALLOWED) { DECLARED_MINUTES_INVALID }
        contextProvider.get().timeToGetUpDataStore.edit { preferences -> preferences[DECLARED_MINUTES] = value }
    }

    private companion object {
        const val DATASTORE_BEFORE_UNLOCK = "DATASTORE_BEFORE_UNLOCK"
        const val DECLARED_MINUTES_INVALID = "DECLARED_MINUTES_INVALID"

        val DECLARED_MINUTES = intPreferencesKey("declared_minutes")
    }
}
