package com.niumi.feature.setup.onboarding

/**
 * État d'affichage pur de [OnboardingScreen]. [canContinue] est dérivé plutôt que stocké : la
 * case cochée est l'unique condition de passage (SPEC_ANDROID §3, dernier point).
 */
data class OnboardingUiState(
    val isAcknowledged: Boolean = false,
) {
    val canContinue: Boolean get() = isAcknowledged
}
