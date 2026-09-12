package com.niumi.feature.session.summary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.WakeScheduleStatusDto
import com.niumi.feature.session.activation.ActivationFailure
import com.niumi.feature.session.activation.ActivationPreview
import com.niumi.feature.session.activation.ArmSessionResult
import com.niumi.feature.session.activation.ArmSessionUseCase
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

        /** Rejoué à chaque `ON_RESUME` : un réglage a pu changer pendant que l'écran était ouvert (§13). */
        fun refresh(
            localTimeIso: String,
            use24Hour: Boolean,
        ) {
            viewModelScope.launch {
                applyPreview(armSessionUseCase.preview(localTimeIso), use24Hour)
            }
        }

        fun activate(localTimeIso: String) {
            if (!state.canActivate) return
            state = state.copy(isActivating = true, message = null)
            viewModelScope.launch {
                when (val result = armSessionUseCase.arm(localTimeIso)) {
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
            state =
                state.copy(
                    display = display,
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
    }
