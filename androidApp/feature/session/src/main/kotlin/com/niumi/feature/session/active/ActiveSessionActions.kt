package com.niumi.feature.session.active

/**
 * Les quatre événements de l'écran 7, regroupés sur le patron de `WakeTimeActions` : un cinquième
 * paramètre (`onOpenRingtone`, étape 27) ferait dépasser `LongParameterList` de detekt si les
 * callbacks restaient à plat.
 */
data class ActiveSessionActions(
    val onModifyOrCancel: () -> Unit,
    val onRemediate: (IncidentPresentation) -> Unit = {},
    val onOpenDiagnostic: () -> Unit = {},
    val onOpenRingtone: () -> Unit = {},
)
