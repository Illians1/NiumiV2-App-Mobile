package com.niumi.feature.setup.accessibility

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.niumi.system.blocking.BlockingController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * SPEC_ANDROID §13 : « recalculer l'état après chaque retour des réglages ». [refresh] est
 * appelé par l'écran sur `ON_RESUME`, jamais automatiquement : aucun clic n'est simulé et aucun
 * réglage n'est modifié à la place de l'utilisateur (§12.3).
 */
@HiltViewModel
class AccessibilityConsentViewModel
    @Inject
    constructor(
        private val blockingController: BlockingController,
    ) : ViewModel() {
        var state by
            mutableStateOf(
                AccessibilityConsentUiState(isServiceEnabled = blockingController.isServiceEnabled()),
            )
            private set

        fun refresh() {
            state = AccessibilityConsentUiState(isServiceEnabled = blockingController.isServiceEnabled())
        }
    }
