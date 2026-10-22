package com.niumi.feature.setup.permissions

import com.niumi.system.permissions.NiumiPermission

/** Les sorties de l'écran 2, groupées comme celles de l'écran 1. */
data class PermissionsActions(
    val onBack: () -> Unit,
    val onAuthorize: (NiumiPermission) -> Unit,
    val onContinue: () -> Unit,
    /** Croix ou toucher à côté d'une fenêtre : la ferme et relit les contrôles. */
    val onDismissDialog: () -> Unit = {},
    /** Bouton de la fenêtre de consentement (§12.3) : ouvre les réglages d'accessibilité. */
    val onOpenAccessibilitySettings: () -> Unit = {},
    /** « J’ai verrouillé Niumi » : ferme la fenêtre et relit les contrôles. */
    val onConfirmRecentsLock: () -> Unit = {},
)
