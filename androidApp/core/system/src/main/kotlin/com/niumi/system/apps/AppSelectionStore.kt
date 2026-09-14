package com.niumi.system.apps

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niumi.database.BlockedPackage
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Provider

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

/**
 * **Garde de déverrouillage (étape 19).** Même motif que
 * [DataStoreSetupPreferences][com.niumi.system.setup.DataStoreSetupPreferences] : ce `DataStore` vit
 * dans le stockage chiffré par les identifiants, et l'instance créée avant le premier
 * déverrouillage continue de servir un état vide une fois l'appareil déverrouillé — mesuré sur
 * appareil le 2026-09-14, la sélection d'applications devenait invisible jusqu'à recréation du
 * processus. La garde empêche l'instance de naître, elle ne se contente pas d'ignorer son résultat.
 *
 * Une sélection vide avant déverrouillage est sans conséquence : `APP_SELECTION` ne fait pas partie
 * des six contrôles surveillés pendant `ARMED` (SPEC_ANDROID §13.1), et les applications bloquées
 * d'une session active sont figées dans Room et dans la projection Direct Boot (§7.2, §7.3), jamais
 * relues ici.
 */
class DataStoreAppSelectionStore(
    private val contextProvider: Provider<Context>,
    private val unlockState: UnlockState,
) : AppSelectionStore {
    override suspend fun selectedCount(): Int = selection().size

    override suspend fun selection(): List<BlockedPackage> = AppSelectionCodec.decode(readRaw())

    override suspend fun replace(selection: List<BlockedPackage>) {
        check(unlockState.isUserUnlocked) { DATASTORE_BEFORE_UNLOCK }
        contextProvider.get().appSelectionDataStore.edit { preferences ->
            preferences[SELECTION_JSON] = AppSelectionCodec.encode(selection)
        }
    }

    private suspend fun readRaw(): String? {
        if (!unlockState.isUserUnlocked) return null
        return contextProvider
            .get()
            .appSelectionDataStore.data
            .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
            .first()[SELECTION_JSON]
    }

    private companion object {
        const val DATASTORE_BEFORE_UNLOCK = "DATASTORE_BEFORE_UNLOCK"

        val SELECTION_JSON = stringPreferencesKey("selection_json")
    }
}
