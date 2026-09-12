package com.niumi.feature.session.active

import com.niumi.core.interop.SessionStateDto
import com.niumi.feature.session.ui.WakeScheduleDisplay

/**
 * État de l'écran 7 (SPEC_ANDROID §15, écran 7). **Version minimale de l'étape 14** : état, date,
 * heure et fuseau d'activation, plus l'heure recalculée si le fuseau courant a changé (§8). La
 * liste des applications bloquées, la santé, les incidents et le bouton « Modifier ou annuler »
 * arrivent à l'étape 15, dans ce même package.
 *
 * [displayInCurrentZone] n'est non nul que si le fuseau courant diffère de celui de l'activation :
 * l'instant ne change jamais après `ACTIVATION_SUCCEEDED` (§8), seule sa lecture locale change.
 */
data class ActiveSessionUiState(
    val state: SessionStateDto? = null,
    val displayAtActivation: WakeScheduleDisplay? = null,
    val displayInCurrentZone: WakeScheduleDisplay? = null,
    val isLoading: Boolean = true,
) {
    val hasSession: Boolean get() = state != null
}
