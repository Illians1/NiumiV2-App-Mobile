package com.niumi.app.navigation

/**
 * État d'affichage de l'accueil. [destination] est calculée par [homeDestinationFor] : l'accueil
 * ne décide pas lui-même où mener, il applique la règle de SPEC_ANDROID §10.4.
 */
data class HomeUiState(
    val destination: NiumiRoute = NiumiRoute.Onboarding,
    val hasActiveSession: Boolean = false,
)
