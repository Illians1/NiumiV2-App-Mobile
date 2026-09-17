package com.niumi.feature.session.wake

/**
 * Les trois événements de l'écran 5, regroupés pour que le composable reste lisible : l'écran est
 * pur, ces lambdas sont son unique moyen de rendre la main au `ViewModel`.
 */
data class WakeTimeActions(
    val onContinue: () -> Unit,
    val onBlockingModeChanged: (Boolean) -> Unit,
    val onBlockingTimeChanged: (Int, Int) -> Unit,
)
