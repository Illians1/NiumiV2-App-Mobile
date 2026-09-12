package com.niumi.feature.session.wake

import com.niumi.feature.session.ui.WakeScheduleDisplay

/** Heure par défaut du cadran tant qu'aucune heure n'a jamais été confirmée (décision utilisateur). */
const val DEFAULT_LOCAL_TIME_ISO = "07:00"

/**
 * État de l'écran 5 (SPEC_ANDROID §15, écran 5 ; SPEC_CORE_KMP §8.1). [display] n'est non nul que
 * si `computeWakeSchedule` a produit un horaire `VALID` pour [localTimeIso] : sinon [message]
 * porte l'explication et [canContinue] reste faux (§15, ne jamais afficher un faux état).
 */
data class WakeTimeUiState(
    val localTimeIso: String = DEFAULT_LOCAL_TIME_ISO,
    val display: WakeScheduleDisplay? = null,
    val use24Hour: Boolean = true,
    val message: String? = null,
    val isSessionInProgress: Boolean = false,
    val isLoading: Boolean = true,
) {
    val canContinue: Boolean get() = display != null && !isSessionInProgress
}
