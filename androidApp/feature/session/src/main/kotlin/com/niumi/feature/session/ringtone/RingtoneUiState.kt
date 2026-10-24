package com.niumi.feature.session.ringtone

import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.Ringtone

/**
 * État de la feuille « Sonnerie » (SPECIFICATION.md §10, étape 40). [isSessionArmed] distingue les
 * deux modes : en `ARMED`, [settings] reflète la session (ce qui sonnera) et chaque changement
 * écrit la préférence **et** la session ; sinon, [settings] reflète la seule préférence.
 * [volumePercent] est le volume du flux d'alarme, affiché sans réglage possible (§10) ; `null`
 * s'il n'est pas lisible.
 */
data class RingtoneUiState(
    val settings: AlarmSoundSettings = AlarmSoundSettings(),
    val ringtones: List<Ringtone> = NiumiRingtones.ALL,
    val previewingKey: String? = null,
    val isAlarmVolumeZero: Boolean = false,
    val isSessionArmed: Boolean = false,
    val volumePercent: Int? = null,
    val message: String? = null,
    val isLoading: Boolean = true,
)
