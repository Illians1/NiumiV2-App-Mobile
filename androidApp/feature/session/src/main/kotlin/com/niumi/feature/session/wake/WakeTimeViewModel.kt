package com.niumi.feature.session.wake

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.WakeScheduleInputDto
import com.niumi.core.interop.WakeScheduleStatusDto
import com.niumi.feature.session.ui.WakeScheduleFormatter
import com.niumi.system.common.Clock
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.isSessionInProgress
import com.niumi.system.setup.SetupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * Choix de l'heure de réveil (écran 5, SPEC_ANDROID §15 ; SPEC_CORE_KMP §8.1). Ne réimplémente
 * aucune règle de calcul : `computeWakeSchedule` de `NiumiCoreFacade` est l'unique autorité,
 * rejouée à chaque changement d'heure et à chaque `ON_RESUME` (le fuseau système a pu changer
 * pendant que l'écran était ouvert, §8).
 */
@HiltViewModel
class WakeTimeViewModel
    @Inject
    constructor(
        private val facade: NiumiCoreFacade,
        private val clock: Clock,
        private val timeZoneProvider: TimeZoneProvider,
        private val setupPreferences: SetupPreferences,
        snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        var state by mutableStateOf(WakeTimeUiState())
            private set

        init {
            viewModelScope.launch {
                state = state.copy(localTimeIso = setupPreferences.lastWakeTimeIso() ?: DEFAULT_LOCAL_TIME_ISO)
                recompute()
            }
            // Garde de session : préparer un second réveil pendant une session en cours mènerait
            // à un refus garanti par `ActivationReducer.onRequested` (SPEC_CORE_KMP §4).
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot ->
                    val inProgress = snapshot?.state.isSessionInProgress()
                    state =
                        state.copy(
                            isSessionInProgress = inProgress,
                            message = if (inProgress) WakeTimeTexts.SESSION_IN_PROGRESS_MESSAGE else state.message,
                        )
                }
            }
        }

        /**
         * Appelé à l'affichage initial et à chaque `ON_RESUME` par la `Route` : [use24Hour] vient
         * de `DateFormat.is24HourFormat`, pour que le cadran et la phrase de confirmation
         * emploient la même convention. Rejoue aussi le calcul, seul moyen de détecter un
         * changement de fuseau système survenu pendant que l'écran était ouvert (§8).
         */
        fun refresh(use24Hour: Boolean) {
            state = state.copy(use24Hour = use24Hour)
            recompute()
        }

        fun onTimeChanged(
            hour: Int,
            minute: Int,
        ) {
            state = state.copy(localTimeIso = LocalTime.of(hour, minute).toString())
            recompute()
        }

        /** Mémorise l'heure choisie (décision utilisateur : le cadran s'en souvient) puis navigue. */
        fun continueToSummary(onContinue: (String) -> Unit) {
            if (!state.canContinue) return
            viewModelScope.launch {
                setupPreferences.setLastWakeTimeIso(state.localTimeIso)
                onContinue(state.localTimeIso)
            }
        }

        private fun recompute() {
            val nowEpochMillis = clock.nowEpochMillis()
            val result =
                facade.computeWakeSchedule(
                    WakeScheduleInputDto(
                        localTimeIso = state.localTimeIso,
                        zoneId = timeZoneProvider.currentZoneId(),
                        nowEpochMillis = nowEpochMillis,
                    ),
                )
            // Brancher sur `schedule` plutôt que sur le statut : `schedule` n'est non nul que pour
            // `VALID`, c'est le contrat de la façade (SPEC_CORE_KMP §14, résultat typé).
            val schedule = result.schedule
            state =
                if (schedule == null) {
                    state.copy(display = null, message = messageFor(result.status), isLoading = false)
                } else {
                    state.copy(
                        display =
                            WakeScheduleFormatter.format(schedule, nowEpochMillis, use24Hour = state.use24Hour),
                        message = null,
                        isLoading = false,
                    )
                }
        }

        /**
         * `INVALID_TIME` n'est pas atteignable depuis le cadran, dont les heures et minutes
         * entières produisent toujours une heure ISO valide. Il est traité tout de même : le
         * contrat de la façade l'autorise, et le taire laisserait l'écran sans explication.
         */
        private fun messageFor(status: WakeScheduleStatusDto): String? =
            when (status) {
                WakeScheduleStatusDto.VALID -> null
                WakeScheduleStatusDto.INVALID_TIME -> WakeTimeTexts.INVALID_TIME_MESSAGE
                WakeScheduleStatusDto.UNKNOWN_ZONE -> WakeTimeTexts.UNKNOWN_ZONE_MESSAGE
            }
    }
