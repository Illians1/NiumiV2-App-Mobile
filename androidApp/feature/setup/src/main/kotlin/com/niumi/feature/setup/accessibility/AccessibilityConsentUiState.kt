package com.niumi.feature.setup.accessibility

/** État d'affichage pur de [AccessibilityConsentScreen], recalculé à chaque `ON_RESUME`. */
data class AccessibilityConsentUiState(
    val isServiceEnabled: Boolean,
)
