package com.niumi.feature.session.ringtone

import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.Ringtone

/**
 * État de l'écran 14 (SPEC_ANDROID §15, Lot 7). [isSessionArmed] distingue les deux modes : en
 * `ARMED`, [settings] reflète la session (ce qui sonnera), le bandeau [RingtoneTexts.ARMED_BANNER]
 * s'affiche, et chaque changement écrit la préférence **et** la session ; sinon, [settings] reflète
 * la seule préférence.
 */
data class RingtoneUiState(
    val settings: AlarmSoundSettings = AlarmSoundSettings(),
    val ringtones: List<Ringtone> = NiumiRingtones.ALL,
    val previewingKey: String? = null,
    val isAlarmVolumeZero: Boolean = false,
    val isSessionArmed: Boolean = false,
    val message: String? = null,
    val isLoading: Boolean = true,
)
