package com.niumi.feature.session.active

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.feature.session.ui.WakeScheduleFormatter
import com.niumi.system.common.Clock
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Écran 7, version minimale de l'étape 14 (SPEC_ANDROID §15). Ne fait que projeter le snapshot
 * publié : l'instant de déclenchement est immuable après `ACTIVATION_SUCCEEDED` (§8), seule sa
 * lecture locale peut changer avec le fuseau du téléphone — d'où deux projections quand les deux
 * fuseaux diffèrent, jamais un recalcul de la session.
 */
@HiltViewModel
class ActiveSessionViewModel
    @Inject
    constructor(
        private val clock: Clock,
        private val timeZoneProvider: TimeZoneProvider,
        private val snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        var state by mutableStateOf(ActiveSessionUiState())
            private set

        /**
         * Convention 12/24 h du système, fournie par la `Route` comme sur les écrans 5 et 6 :
         * trois écrans portent la même heure, ils ne peuvent pas employer deux conventions (§15).
         */
        private var use24Hour = true

        init {
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot -> state = project(snapshot) }
            }
        }

        fun refresh(use24Hour: Boolean) {
            this.use24Hour = use24Hour
            state = project(snapshotPublisher.snapshot.value)
        }

        private fun project(snapshot: SessionSnapshotDto?): ActiveSessionUiState {
            if (snapshot == null) return ActiveSessionUiState(isLoading = false)
            val nowEpochMillis = clock.nowEpochMillis()
            val currentZoneId = timeZoneProvider.currentZoneId()
            val schedule = snapshot.wakeSchedule
            return ActiveSessionUiState(
                state = snapshot.state,
                displayAtActivation = WakeScheduleFormatter.format(schedule, nowEpochMillis, use24Hour = use24Hour),
                displayInCurrentZone =
                    currentZoneId
                        .takeIf { it != schedule.zoneIdAtActivation }
                        ?.let {
                            WakeScheduleFormatter.format(
                                schedule,
                                nowEpochMillis,
                                displayZoneId = it,
                                use24Hour = use24Hour,
                            )
                        },
                isLoading = false,
            )
        }
    }
