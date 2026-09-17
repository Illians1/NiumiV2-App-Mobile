package com.niumi.feature.session.summary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.diagnostics.ActivationReasonCode
import com.niumi.core.interop.BlockingScheduleStatusDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.WakeScheduleStatusDto
import com.niumi.feature.session.activation.ActivationFailure
import com.niumi.feature.session.activation.ActivationPreview
import com.niumi.feature.session.activation.ArmSessionResult
import com.niumi.feature.session.activation.ArmSessionUseCase
import com.niumi.feature.session.ui.BlockingScheduleFormatter
import com.niumi.feature.session.ui.WakeScheduleFormatter
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.isSessionInProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Récapitulatif d'engagement (écran 6, SPEC_ANDROID §15 ; §9.2 pour l'activation). Ne décide rien
 * lui-même : `ArmSessionUseCase.preview` porte le verdict affiché, `arm` porte l'activation, et
 * les deux rejouent le même diagnostic — l'aperçu affiché n'est jamais réutilisé pour armer
 * (SPEC_CORE_KMP §8.1, recalcul du fuseau entre saisie et confirmation).
 */
@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        private val armSessionUseCase: ArmSessionUseCase,
        snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        var state by mutableStateOf(SummaryUiState())
            private set

        /** Non nul dès que l'activation a réussi : la `Route` s'en sert pour naviguer une seule fois. */
        var armedSnapshot: SessionSnapshotDto? = null
            private set

        init {
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot ->
                    // Un état final n'est pas une session en cours : le publisher conserve le
                    // dernier snapshot pour l'écran de fin (SPEC_CORE_KMP §5.1).
                    state = state.copy(isSessionInProgress = snapshot?.state.isSessionInProgress())
                }
            }
        }

        /**
         * Rejoué à chaque `ON_RESUME` : un réglage a pu changer pendant que l'écran était ouvert
         * (§13). [blockingLocalTimeIso] est le choix de l'écran 5, nul pour un blocage immédiat —
         * jamais l'instant calculé, qui est recalculé ici comme l'heure de réveil (§8.3).
         */
        fun refresh(
            localTimeIso: String,
            blockingLocalTimeIso: String?,
            use24Hour: Boolean,
        ) {
            viewModelScope.launch {
                applyPreview(armSessionUseCase.preview(localTimeIso, blockingLocalTimeIso), use24Hour)
            }
        }

        fun activate(
            localTimeIso: String,
            blockingLocalTimeIso: String?,
        ) {
            if (!state.canActivate) return
            state = state.copy(isActivating = true, message = null)
            viewModelScope.launch {
                when (val result = armSessionUseCase.arm(localTimeIso, blockingLocalTimeIso)) {
                    is ArmSessionResult.Armed -> {
                        armedSnapshot = result.snapshot
                        state = state.copy(isActivating = false)
                    }

                    is ArmSessionResult.Failed -> {
                        state = state.copy(isActivating = false, message = messageFor(result.failure))
                    }
                }
            }
        }

        private fun applyPreview(
            preview: ActivationPreview,
            use24Hour: Boolean,
        ) {
            val schedule = preview.scheduleResult.schedule
            val display =
                schedule?.let {
                    WakeScheduleFormatter.format(it, preview.nowEpochMillis, use24Hour = use24Hour)
                }
            // Le fuseau du début du blocage est celui du réveil : une session n'en a qu'un
            // (SPEC_CORE_KMP §7.5).
            val blockingDisplay =
                schedule?.let { wakeSchedule ->
                    preview.blockingResult?.schedule?.let { blockingSchedule ->
                        BlockingScheduleFormatter.format(
                            schedule = blockingSchedule,
                            zoneIdAtActivation = wakeSchedule.zoneIdAtActivation,
                            nowEpochMillis = preview.nowEpochMillis,
                            use24Hour = use24Hour,
                        )
                    }
                }
            state =
                state.copy(
                    display = display,
                    blockingDisplay = blockingDisplay,
                    blockedPackages = preview.blockedPackages,
                    boxId = preview.boxId,
                    isAllowed = preview.canActivate,
                    isLoading = false,
                    message = previewMessage(preview),
                )
        }

        private fun previewMessage(preview: ActivationPreview): String? =
            when {
                preview.scheduleResult.status != WakeScheduleStatusDto.VALID -> {
                    SummaryTexts.INVALID_SCHEDULE_MESSAGE
                }

                // Un début de blocage refusé n'atteint pas la politique commune : le calcul n'a
                // produit aucun instant à lui soumettre (§13, point 4). Sans cette branche, l'écran
                // désactiverait « Activer ma session » sans dire pourquoi.
                preview.blockingResult != null &&
                    preview.blockingResult.status != BlockingScheduleStatusDto.VALID -> {
                    blockingScheduleMessage(preview.blockingResult.status)
                }

                preview.canActivate -> {
                    null
                }

                else -> {
                    preview.policy
                        ?.blockingReasons
                        ?.firstOrNull()
                        ?.let { SummaryTexts.blockingReason(it.code) }
                }
            }

        private fun messageFor(failure: ActivationFailure): String =
            when (failure) {
                is ActivationFailure.Blocked -> {
                    failure.reasons.firstOrNull()?.let { SummaryTexts.blockingReason(it.code) }
                        ?: SummaryTexts.activationFailed(failureCode = null)
                }

                is ActivationFailure.InvalidSchedule -> {
                    SummaryTexts.INVALID_SCHEDULE_MESSAGE
                }

                is ActivationFailure.InvalidBlockingSchedule -> {
                    blockingScheduleMessage(failure.status)
                }

                is ActivationFailure.CoordinatorFailed -> {
                    SummaryTexts.activationFailed(failure.failureCode)
                }

                is ActivationFailure.Rejected -> {
                    SummaryTexts.REJECTED_MESSAGE
                }

                ActivationFailure.Duplicate -> {
                    SummaryTexts.DUPLICATE_MESSAGE
                }
            }

        /**
         * Un début de blocage refusé, que le refus vienne de l'aperçu ou de la confirmation : le
         * même fait ne peut pas être nommé de deux façons (§15).
         */
        private fun blockingScheduleMessage(status: BlockingScheduleStatusDto): String =
            when (status) {
                BlockingScheduleStatusDto.NOT_BEFORE_TRIGGER -> {
                    SummaryTexts.blockingReason(ActivationReasonCode.BLOCKING_START_NOT_BEFORE_TRIGGER)
                }

                BlockingScheduleStatusDto.VALID,
                BlockingScheduleStatusDto.INVALID_TIME,
                BlockingScheduleStatusDto.UNKNOWN_ZONE,
                -> {
                    SummaryTexts.INVALID_SCHEDULE_MESSAGE
                }
            }
    }
