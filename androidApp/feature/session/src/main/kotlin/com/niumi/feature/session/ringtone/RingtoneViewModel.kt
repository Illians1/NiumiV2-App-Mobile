package com.niumi.feature.session.ringtone

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.audio.RingtonePreviewPlayer
import com.niumi.system.audio.VolumeRampDurations
import com.niumi.system.common.OperationResult
import com.niumi.system.session.AlarmSoundUpdateResult
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionPersistenceGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Écran 14 (SPEC_ANDROID §15, Lot 7). Deux modes, décidés par la session plutôt que par un
 * argument de route (étape 27, faiblesse de spec signalée : §15 ne précise pas comment l'écran sait
 * dans quel mode se présenter) :
 *
 * - une session `Present` en `ARMED` : [RingtoneUiState.settings] reflète **ce qui sonnera**, et
 *   chaque changement écrit la préférence puis appelle [SessionCoordinator.updateAlarmSound] ;
 * - sinon : [RingtoneUiState.settings] reflète la seule préférence hors session.
 */
@HiltViewModel
class RingtoneViewModel
    @Inject
    constructor(
        private val preferences: AlarmSoundPreferences,
        private val previewPlayer: RingtonePreviewPlayer,
        private val alarmVolumeSource: AlarmVolumeSource,
        private val coordinator: SessionCoordinator,
        private val gateway: SessionPersistenceGateway,
    ) : ViewModel() {
        var state by mutableStateOf(RingtoneUiState())
            private set

        init {
            viewModelScope.launch { load() }
            // La fin naturelle d'une pré-écoute (sans boucle, §10.2) doit faire disparaître l'icône
            // « arrêt » sans action de l'utilisateur : sans ce flux, elle resterait affichée.
            viewModelScope.launch {
                previewPlayer.playingKey.collect { key -> state = state.copy(previewingKey = key) }
            }
        }

        /** Appelé à `ON_RESUME` : le volume d'alarme a pu changer, et la session a pu quitter `ARMED`. */
        fun refresh() {
            viewModelScope.launch { load() }
        }

        private suspend fun load() {
            val sessionSettings = sessionArmedSettings()
            state =
                state.copy(
                    settings = sessionSettings ?: preferences.read().sanitized(),
                    isSessionArmed = sessionSettings != null,
                    isAlarmVolumeZero = alarmVolumeSource.alarmStreamVolume() <= 0,
                    isLoading = false,
                )
        }

        private suspend fun sessionArmedSettings(): AlarmSoundSettings? {
            val loaded = gateway.load()
            if (loaded !is LoadResult.Present || loaded.snapshot.state != SessionStateDto.ARMED) return null
            return AlarmSoundSettings(loaded.extras.ringtoneKey, loaded.extras.volumeRampSeconds).sanitized()
        }

        fun onRingtoneSelected(key: String) = apply(state.settings.copy(ringtoneKey = key))

        fun onVolumeRampEnabledChanged(enabled: Boolean) =
            apply(
                state.settings.copy(
                    volumeRampSeconds = if (enabled) VolumeRampDurations.DEFAULT_SECONDS else null,
                ),
            )

        fun onVolumeRampSecondsChanged(seconds: Int) = apply(state.settings.copy(volumeRampSeconds = seconds))

        /**
         * Toujours la préférence d'abord, la session ensuite (§15) : un échec de l'une ne doit pas
         * empêcher l'autre. [RingtoneTexts.NOT_ARMED_MESSAGE] et [RingtoneTexts.SESSION_UPDATE_FAILED]
         * couvrent ce que §15 ne nomme pas (voir `ETAPE-27.md`).
         */
        private fun apply(newSettings: AlarmSoundSettings) {
            val sanitized = newSettings.sanitized()
            val wasArmed = state.isSessionArmed
            state = state.copy(settings = sanitized, message = null)
            viewModelScope.launch {
                val written = runCatching { preferences.write(sanitized) }
                if (written.isFailure) {
                    state = state.copy(message = RingtoneTexts.SAVE_FAILED)
                    return@launch
                }
                if (wasArmed) applyToSession(sanitized)
            }
        }

        private suspend fun applyToSession(settings: AlarmSoundSettings) {
            when (coordinator.updateAlarmSound(settings.ringtoneKey, settings.volumeRampSeconds)) {
                AlarmSoundUpdateResult.Updated -> {}

                AlarmSoundUpdateResult.NotArmed -> {
                    state = state.copy(isSessionArmed = false, message = RingtoneTexts.NOT_ARMED_MESSAGE)
                }

                AlarmSoundUpdateResult.NoActiveSession -> {
                    state = state.copy(isSessionArmed = false)
                }

                AlarmSoundUpdateResult.UnknownRingtone,
                AlarmSoundUpdateResult.InvalidRampDuration,
                AlarmSoundUpdateResult.DeferredUntilUnlock,
                -> {
                    state = state.copy(message = RingtoneTexts.SESSION_UPDATE_FAILED)
                }
            }
        }

        /**
         * Relit le volume à chaque geste (§15 : le message dépend de l'instant présent, pas d'un
         * état mis en cache à l'ouverture de l'écran).
         */
        fun onPreviewToggled(key: String) {
            if (previewPlayer.playingKey.value == key) {
                previewPlayer.stop()
                return
            }
            if (alarmVolumeSource.alarmStreamVolume() <= 0) {
                state = state.copy(isAlarmVolumeZero = true)
                return
            }
            val result = previewPlayer.play(key)
            if (result is OperationResult.Failure) state = state.copy(message = RingtoneTexts.PREVIEW_FAILED)
        }

        /** Appelé à `ON_PAUSE` par l'écran (§15 : la pré-écoute s'arrête en quittant l'écran). */
        fun onPause() = previewPlayer.stop()

        override fun onCleared() {
            previewPlayer.stop()
        }
    }
