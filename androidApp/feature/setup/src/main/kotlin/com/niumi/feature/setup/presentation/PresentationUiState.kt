package com.niumi.feature.setup.presentation

/** État d'affichage de l'écran 1 : le panneau affiché, 1 ou 2. */
data class PresentationUiState(
    val panel: Int = FIRST_PANEL,
) {
    val isLastPanel: Boolean get() = panel == LAST_PANEL

    companion object {
        const val FIRST_PANEL = 1
        const val LAST_PANEL = 2
    }
}

/** Les trois gestes de l'écran 1. */
data class PresentationActions(
    val onContinue: () -> Unit,
    val onBack: () -> Unit,
    val onConfigure: () -> Unit,
)
