package com.niumi.feature.session.active

/**
 * Les événements de l'écran 7, regroupés sur le patron de `WakeTimeActions` : un cinquième
 * paramètre (`onOpenRingtone`, étape 27) ferait dépasser `LongParameterList` de detekt si les
 * callbacks restaient à plat. [onOpenBoxes] (étape 43) ouvre l'écran 3 actuel en session, de façon
 * transitoire jusqu'à l'étape 45.
 */
data class ActiveSessionActions(
    val onModifyOrCancel: () -> Unit,
    val onRemediate: (IncidentPresentation) -> Unit = {},
    val onOpenDiagnostic: () -> Unit = {},
    val onOpenRingtone: () -> Unit = {},
    val onOpenBoxes: () -> Unit = {},
)
