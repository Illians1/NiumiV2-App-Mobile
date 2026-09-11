package com.niumi.system.apps

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niumi.database.BlockedPackage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Sélection d'applications **courante**, hors session (SPEC_ANDROID §12.1). Distincte des
 * `BlockedAppEntity` de Room, qui appartiennent à une session activée et ne changent plus
 * (SPEC_CORE_KMP §2 point 11) : ce dépôt est ce que l'utilisateur prépare, celui de Room est ce
 * qui est engagé. L'étape 14 recopie l'un dans l'autre au moment de l'activation.
 *
 * Hors Room comme [SetupPreferences][com.niumi.system.setup.SetupPreferences] : la sélection doit
 * rester lisible et modifiable sans session, sans migration de schéma.
 */
interface AppSelectionStore : AppSelectionSource {
    suspend fun selection(): List<BlockedPackage>

    suspend fun replace(selection: List<BlockedPackage>)
}

private val Context.appSelectionDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "niumi_app_selection")

class DataStoreAppSelectionStore(
    private val context: Context,
) : AppSelectionStore {
    override suspend fun selectedCount(): Int = selection().size

    override suspend fun selection(): List<BlockedPackage> = AppSelectionCodec.decode(readRaw())

    override suspend fun replace(selection: List<BlockedPackage>) {
        context.appSelectionDataStore.edit { preferences ->
            preferences[SELECTION_JSON] = AppSelectionCodec.encode(selection)
        }
    }

    private suspend fun readRaw(): String? =
        context.appSelectionDataStore.data
            .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
            .first()[SELECTION_JSON]

    private companion object {
        val SELECTION_JSON = stringPreferencesKey("selection_json")
    }
}
