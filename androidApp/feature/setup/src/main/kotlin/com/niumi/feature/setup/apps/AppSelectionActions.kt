package com.niumi.feature.setup.apps

/** Les sorties de l'écran 4 et de sa page, groupées comme celles des écrans 1 et 2. */
data class AppSelectionActions(
    val onOpenPage: () -> Unit,
    val onContinue: () -> Unit,
    val onToggle: (String) -> Unit = {},
    val onSearch: (String) -> Unit = {},
    /** Croix, voile, glissement ou retour : abandonne le brouillon. */
    val onDismissPage: () -> Unit = {},
    val onValidate: () -> Unit = {},
)
