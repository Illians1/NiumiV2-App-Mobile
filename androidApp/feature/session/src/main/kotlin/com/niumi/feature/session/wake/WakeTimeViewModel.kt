package com.niumi.feature.session.wake

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.BlockingScheduleInputDto
import com.niumi.core.interop.BlockingScheduleStatusDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.WakeScheduleInputDto
import com.niumi.core.interop.WakeScheduleStatusDto
import com.niumi.feature.session.ui.BlockingScheduleFormatter
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
 * Choix de l'heure de réveil et du début du blocage (écran 5, SPEC_ANDROID §15 ; SPEC_CORE_KMP
 * §8.1, §8.3). Ne réimplémente aucune règle de calcul : `computeWakeSchedule` et
 * `computeBlockingSchedule` de `NiumiCoreFacade` sont l'unique autorité, rejouées à chaque
 * changement de l'une ou l'autre heure et à chaque `ON_RESUME` (le fuseau système a pu changer
 * pendant que l'écran était ouvert, §8).
 *
 * Les deux calculs partagent un seul `nowEpochMillis` et un seul fuseau, comme l'exige §8.3 :
 * comparer un début de blocage à un réveil calculé sur un autre instant de référence produirait un
 * verdict d'antériorité faux.
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

        /**
         * Dernier début différé connu de cet écran : ce que « À partir de » restaure quand
         * l'utilisateur revient sur son choix après être passé par « Maintenant ». `null` tant
         * qu'aucun début n'a jamais été confirmé ni saisi, cas où la sélection ouvre simplement le
         * sélecteur d'heure (décision du 2026-09-17, SPEC_ANDROID §15).
         */
        private var lastDeferredBlockingTimeIso: String? = null

        init {
            viewModelScope.launch {
                lastDeferredBlockingTimeIso = setupPreferences.lastBlockingStartTimeIso()
                state =
                    state.copy(
                        localTimeIso = setupPreferences.lastWakeTimeIso() ?: DEFAULT_LOCAL_TIME_ISO,
                        blockingLocalTimeIso = lastDeferredBlockingTimeIso,
                    )
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

        /**
         * Bascule « Maintenant » / « À partir de » (§15). Passer au différé sans heure connue ne
         * change pas l'état : c'est l'écran qui ouvre alors le sélecteur, et l'annuler doit laisser
         * « Maintenant » sélectionné plutôt qu'un « À partir de » sans heure.
         */
        fun onBlockingModeChanged(immediate: Boolean) {
            state =
                if (immediate) {
                    state.copy(blockingLocalTimeIso = null)
                } else {
                    state.copy(blockingLocalTimeIso = state.blockingLocalTimeIso ?: lastDeferredBlockingTimeIso)
                }
            recompute()
        }

        fun onBlockingTimeChanged(
            hour: Int,
            minute: Int,
        ) {
            val localTimeIso = LocalTime.of(hour, minute).toString()
            lastDeferredBlockingTimeIso = localTimeIso
            state = state.copy(blockingLocalTimeIso = localTimeIso)
            recompute()
        }

        /**
         * Mémorise les deux choix (décision utilisateur : l'écran s'en souvient) puis navigue. Un
         * blocage immédiat **efface** la clé du début différé, « Maintenant » étant le défaut tant
         * qu'aucun début n'a été confirmé (§15).
         */
        fun continueToSummary(onContinue: (String, String?) -> Unit) {
            if (!state.canContinue) return
            val localTimeIso = state.localTimeIso
            val blockingLocalTimeIso = state.blockingLocalTimeIso
            viewModelScope.launch {
                setupPreferences.setLastWakeTimeIso(localTimeIso)
                setupPreferences.setLastBlockingStartTimeIso(blockingLocalTimeIso)
                onContinue(localTimeIso, blockingLocalTimeIso)
            }
        }

        private fun recompute() {
            val nowEpochMillis = clock.nowEpochMillis()
            val zoneId = timeZoneProvider.currentZoneId()
            val result =
                facade.computeWakeSchedule(
                    WakeScheduleInputDto(
                        localTimeIso = state.localTimeIso,
                        zoneId = zoneId,
                        nowEpochMillis = nowEpochMillis,
                    ),
                )
            // Brancher sur `schedule` plutôt que sur le statut : `schedule` n'est non nul que pour
            // `VALID`, c'est le contrat de la façade (SPEC_CORE_KMP §14, résultat typé).
            val schedule = result.schedule
            state =
                if (schedule == null) {
                    // Sans réveil valide, il n'existe aucun repère pour juger l'antériorité du
                    // début du blocage (§8.3) : l'écran n'affirme alors rien à son sujet.
                    state.copy(
                        display = null,
                        message = messageFor(result.status),
                        blockingDisplay = null,
                        blockingMessage = null,
                        isLoading = false,
                    )
                } else {
                    withBlocking(
                        base =
                            state.copy(
                                display =
                                    WakeScheduleFormatter.format(
                                        schedule,
                                        nowEpochMillis,
                                        use24Hour = state.use24Hour,
                                    ),
                                message = null,
                                isLoading = false,
                            ),
                        zoneId = zoneId,
                        nowEpochMillis = nowEpochMillis,
                        triggerAtEpochMillis = schedule.triggerAtEpochMillis,
                    )
                }
        }

        /**
         * Calcule le début du blocage sur le réveil qui vient d'être obtenu (§8.3). Un blocage
         * immédiat passe par le même chemin : la façade rend alors `VALID` avec un schedule aux
         * trois champs nuls, que `BlockingScheduleFormatter` traduit par « rien à afficher ».
         */
        private fun withBlocking(
            base: WakeTimeUiState,
            zoneId: String,
            nowEpochMillis: Long,
            triggerAtEpochMillis: Long,
        ): WakeTimeUiState {
            val result =
                facade.computeBlockingSchedule(
                    BlockingScheduleInputDto(
                        localTimeIso = base.blockingLocalTimeIso,
                        zoneId = zoneId,
                        nowEpochMillis = nowEpochMillis,
                        triggerAtEpochMillis = triggerAtEpochMillis,
                    ),
                )
            return base.copy(
                blockingDisplay =
                    result.schedule?.let { schedule ->
                        BlockingScheduleFormatter.format(
                            schedule = schedule,
                            zoneIdAtActivation = zoneId,
                            nowEpochMillis = nowEpochMillis,
                            use24Hour = base.use24Hour,
                        )
                    },
                blockingMessage = blockingMessageFor(result.status),
            )
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

        /**
         * `NOT_BEFORE_TRIGGER` est le seul refus atteignable depuis le sélecteur ; les deux autres
         * sont traités par symétrie avec le réveil, le contrat de la façade les autorisant
         * (SPEC_CORE_KMP §14).
         */
        private fun blockingMessageFor(status: BlockingScheduleStatusDto): String? =
            when (status) {
                BlockingScheduleStatusDto.VALID -> null
                BlockingScheduleStatusDto.NOT_BEFORE_TRIGGER -> WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE
                BlockingScheduleStatusDto.INVALID_TIME -> WakeTimeTexts.INVALID_TIME_MESSAGE
                BlockingScheduleStatusDto.UNKNOWN_ZONE -> WakeTimeTexts.UNKNOWN_ZONE_MESSAGE
            }
    }
