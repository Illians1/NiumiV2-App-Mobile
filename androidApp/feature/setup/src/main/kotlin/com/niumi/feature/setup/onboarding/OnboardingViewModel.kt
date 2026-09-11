package com.niumi.feature.setup.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.system.setup.SetupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * L'accusé de réception n'est persisté qu'au moment où l'utilisateur poursuit, jamais au clic sur
 * la case : cocher puis quitter l'écran ne vaut pas lecture des limites.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val setupPreferences: SetupPreferences,
    ) : ViewModel() {
        var state by mutableStateOf(OnboardingUiState())
            private set

        fun setAcknowledged(acknowledged: Boolean) {
            state = state.copy(isAcknowledged = acknowledged)
        }

        fun confirm(onConfirmed: () -> Unit) {
            if (!state.canContinue) return
            viewModelScope.launch {
                setupPreferences.acknowledgeOnboarding()
                onConfirmed()
            }
        }
    }
