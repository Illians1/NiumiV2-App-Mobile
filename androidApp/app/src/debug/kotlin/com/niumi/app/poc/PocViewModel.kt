package com.niumi.app.poc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.common.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Session fictive de la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0). L'identifiant est
 * un UUID canonique fixe : `ServiceCommand.from` (`:feature:ringing`) refuse toute autre forme
 * quand `AlarmReceiver` reçoit le broadcast programmé par `AlarmScheduler`.
 */
private const val POC_SESSION_ID = "00000000-0000-4000-8000-000000000000"
private const val POC_REVISION = 1L

data class PocUiState(
    val secondsInput: String = "90",
    val isScheduled: Boolean = false,
)

@HiltViewModel
class PocViewModel
    @Inject
    constructor(
        private val alarmScheduler: AlarmScheduler,
        private val clock: Clock,
    ) : ViewModel() {
        var state by mutableStateOf(PocUiState(isScheduled = alarmScheduler.isScheduled(POC_SESSION_ID)))
            private set

        fun onSecondsInputChanged(value: String) {
            state = state.copy(secondsInput = value.filter { it.isDigit() })
        }

        fun schedule() {
            val seconds = state.secondsInput.toLongOrNull() ?: return
            val triggerAtEpochMillis = clock.nowEpochMillis() + seconds * MILLIS_PER_SECOND
            alarmScheduler.schedule(POC_SESSION_ID, POC_REVISION, triggerAtEpochMillis)
            refresh()
        }

        fun cancel() {
            alarmScheduler.cancel(POC_SESSION_ID)
            refresh()
        }

        private fun refresh() {
            state = state.copy(isScheduled = alarmScheduler.isScheduled(POC_SESSION_ID))
        }

        private companion object {
            const val MILLIS_PER_SECOND = 1000L
        }
    }
