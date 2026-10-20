package com.niumi.feature.setup.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.feature.setup.presentation.PresentationUiState.Companion.FIRST_PANEL
import com.niumi.feature.setup.presentation.PresentationUiState.Companion.LAST_PANEL
import com.niumi.system.setup.SetupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Écran 1 (`SPECIFICATION.md` §4) : deux panneaux qu'on ne peut pas passer, c'est le seul endroit où
 * l'utilisateur apprend qu'une session démarre sans lui. Le panneau affiché vit dans le
 * [SavedStateHandle] : il survit à la rotation comme à une mort du processus.
 *
 * L'accusé est écrit par « Configurer Niumi », sur le panneau 2 seulement, et une seule fois : un
 * double tap ne navigue pas deux fois. La clé reste `onboarding_acknowledged` : la renommer ferait
 * revenir la présentation chez les utilisateurs qui avaient lu l'ancien onboarding.
 */
@HiltViewModel
class PresentationViewModel
    @Inject
    constructor(
        private val setupPreferences: SetupPreferences,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private var acknowledging = false

        var state by mutableStateOf(PresentationUiState(panel = savedStateHandle[PANEL_KEY] ?: FIRST_PANEL))
            private set

        fun next() = show(LAST_PANEL)

        fun back() = show(FIRST_PANEL)

        fun acknowledge(onAcknowledged: () -> Unit) {
            if (!state.isLastPanel || acknowledging) return
            acknowledging = true
            viewModelScope.launch {
                setupPreferences.acknowledgeOnboarding()
                onAcknowledged()
            }
        }

        private fun show(panel: Int) {
            savedStateHandle[PANEL_KEY] = panel
            state = PresentationUiState(panel = panel)
        }

        private companion object {
            const val PANEL_KEY = "panel"
        }
    }
