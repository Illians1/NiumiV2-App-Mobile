package com.niumi.system.blocking

/**
 * Diagnostic de l'activation de `NiumiBlockingAccessibilityService` (SPEC_ANDROID §13 : contrôle
 * « service d'accessibilité actif »). L'implémentation Android vit dans `:feature:session`, qui
 * seul connaît le nom qualifié du service ; `:core:system` ne peut pas y référencer une classe
 * d'un module downstream (SPEC_ANDROID §6).
 */
interface AccessibilityServiceStatus {
    fun isEnabled(): Boolean
}
