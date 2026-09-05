package com.niumi.app.poc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.common.Clock
import com.niumi.system.pairing.PairedBoxStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// SPEC_ANDROID §16 : jamais le token ni son hash complet, seulement un préfixe de boxId.
private const val BOX_ID_PREFIX_LENGTH = 8

data class PocUiState(
    val secondsInput: String = "90",
    val isScheduled: Boolean = false,
    val pairedBoxIdPrefix: String? = null,
)

@HiltViewModel
class PocViewModel
    @Inject
    constructor(
        private val alarmScheduler: AlarmScheduler,
        private val clock: Clock,
        private val pairedBoxStore: PairedBoxStore,
    ) : ViewModel() {
        var state by mutableStateOf(PocUiState(isScheduled = alarmScheduler.isScheduled(PocSession.ID)))
            private set

        init {
            viewModelScope.launch {
                state = state.copy(pairedBoxIdPrefix = pairedBoxStore.current()?.boxId?.take(BOX_ID_PREFIX_LENGTH))
            }
        }

        fun onSecondsInputChanged(value: String) {
            state = state.copy(secondsInput = value.filter { it.isDigit() })
        }

        /** Recharge le boîtier associé (appelé au retour de `PocPairingActivity`). */
        fun refreshPairedBox() {
            viewModelScope.launch {
                state = state.copy(pairedBoxIdPrefix = pairedBoxStore.current()?.boxId?.take(BOX_ID_PREFIX_LENGTH))
            }
        }

        fun schedule() {
            val seconds = state.secondsInput.toLongOrNull() ?: return
            val triggerAtEpochMillis = clock.nowEpochMillis() + seconds * MILLIS_PER_SECOND
            alarmScheduler.schedule(PocSession.ID, PocSession.REVISION, triggerAtEpochMillis)
            refresh()
        }

        fun cancel() {
            alarmScheduler.cancel(PocSession.ID)
            refresh()
        }

        private fun refresh() {
            state = state.copy(isScheduled = alarmScheduler.isScheduled(PocSession.ID))
        }

        private companion object {
            const val MILLIS_PER_SECOND = 1000L
        }
    }
