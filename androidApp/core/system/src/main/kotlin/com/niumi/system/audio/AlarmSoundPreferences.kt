package com.niumi.system.audio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Provider

/**
 * Dernier choix de sonnerie et de montée de volume, hors session (SPEC_ANDROID §15, Lot 7). Relu
 * **au moment d'armer** une session, jamais transporté par la navigation (plan maître, étape 27) ;
 * distinct de `SetupPreferences`, déjà au plafond `TooManyFunctions` de detekt (`ETAPE-24.md`).
 *
 * [ringtoneKey] et [volumeRampSeconds] ne sont pas garantis valides tels que lus : une clé ou une
 * durée qui a quitté le catalogue (sonnerie retirée, liste de durées resserrée) doit être corrigée
 * par [sanitized] avant tout usage, jamais propagée telle quelle — même principe que
 * `RingingSoundResolver` pour ce qui sonne réellement.
 */
data class AlarmSoundSettings(
    val ringtoneKey: String = NiumiRingtones.DEFAULT_KEY,
    val volumeRampSeconds: Int? = VolumeRampDurations.DEFAULT_SECONDS,
) {
    fun sanitized(): AlarmSoundSettings =
        AlarmSoundSettings(
            ringtoneKey = if (NiumiRingtones.byKey(ringtoneKey) != null) ringtoneKey else NiumiRingtones.DEFAULT_KEY,
            volumeRampSeconds =
                volumeRampSeconds?.let {
                    if (it in VolumeRampDurations.SECONDS) it else VolumeRampDurations.DEFAULT_SECONDS
                },
        )
}

/**
 * Traduit ce que le `DataStore` contient littéralement en [AlarmSoundSettings], hors de toute
 * dépendance Android pour rester testable en JVM.
 *
 * **Défaut mesuré sur appareil le 2026-10-02 (étape 27).** `ringtoneKey` absent ne veut pas dire la
 * même chose que `volumeRampSeconds` absent : le premier signifie « rien n'a jamais été écrit »
 * (défauts complets, montée comprise), le second signifie « volume constant choisi » — [write] les
 * écrit toujours ensemble, [ringtoneKey] ne peut donc être absent qu'avant toute écriture. Les
 * confondre avait fait lire « Piano · volume constant » au lieu du vrai défaut « Piano · volume
 * progressif sur 2 min » à la toute première lecture de la préférence.
 */
internal fun decodeAlarmSoundSettings(
    ringtoneKey: String?,
    volumeRampSeconds: Int?,
): AlarmSoundSettings {
    if (ringtoneKey == null) return AlarmSoundSettings()
    return AlarmSoundSettings(ringtoneKey, volumeRampSeconds).sanitized()
}

interface AlarmSoundPreferences {
    suspend fun read(): AlarmSoundSettings

    suspend fun write(settings: AlarmSoundSettings)
}

private val Context.alarmSoundDataStore: DataStore<Preferences> by preferencesDataStore(name = "niumi_alarm_sound")

/**
 * Même garde de déverrouillage que `DataStoreSetupPreferences` et `DataStoreAppSelectionStore`
 * (point de vigilance 10) : [contextProvider] n'est jamais résolu avant déverrouillage, lecture
 * neutre (les défauts de [AlarmSoundSettings]), écriture refusée.
 *
 * Le volume constant (`volumeRampSeconds = null`) est encodé par l'**absence** de la clé entière :
 * une valeur `0` n'appartient à aucune liste fermée et serait, elle, assainie par [sanitized] vers
 * le défaut — encoder « constant » par `0` romprait donc le aller-retour.
 */
class DataStoreAlarmSoundPreferences(
    private val contextProvider: Provider<Context>,
    private val unlockState: UnlockState,
) : AlarmSoundPreferences {
    override suspend fun read(): AlarmSoundSettings {
        val preferences = preferences() ?: return AlarmSoundSettings()
        return decodeAlarmSoundSettings(preferences[RINGTONE_KEY], preferences[VOLUME_RAMP_SECONDS])
    }

    override suspend fun write(settings: AlarmSoundSettings) {
        check(unlockState.isUserUnlocked) { DATASTORE_BEFORE_UNLOCK }
        val sanitized = settings.sanitized()
        contextProvider.get().alarmSoundDataStore.edit { preferences ->
            preferences[RINGTONE_KEY] = sanitized.ringtoneKey
            if (sanitized.volumeRampSeconds == null) {
                preferences.remove(VOLUME_RAMP_SECONDS)
            } else {
                preferences[VOLUME_RAMP_SECONDS] = sanitized.volumeRampSeconds
            }
        }
    }

    private suspend fun preferences(): Preferences? {
        if (!unlockState.isUserUnlocked) return null
        return contextProvider
            .get()
            .alarmSoundDataStore.data
            .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
            .first()
    }

    private companion object {
        const val DATASTORE_BEFORE_UNLOCK = "DATASTORE_BEFORE_UNLOCK"

        val RINGTONE_KEY = stringPreferencesKey("ringtone_key")
        val VOLUME_RAMP_SECONDS = intPreferencesKey("volume_ramp_seconds")
    }
}
