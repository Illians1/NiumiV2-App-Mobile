package com.niumi.system.blocking

/**
 * Diagnostic de l'activation de `NiumiBlockingAccessibilityService` (SPEC_ANDROID §13 : contrôle
 * « service d'accessibilité actif »). L'implémentation Android vit dans `:feature:session`, qui
 * seul connaît le nom qualifié du service ; `:core:system` ne peut pas y référencer une classe
 * d'un module downstream (SPEC_ANDROID §6).
 */
interface AccessibilityServiceStatus {
    fun isEnabled(): Boolean

    /**
     * L'état fin du service (étape 25) : [isEnabled] ne distingue pas « retiré de la liste par
     * l'utilisateur » de « inscrit, mais pas encore relié par Android ». Par défaut, dérivé de
     * [isEnabled] : une implémentation qui ne sait pas faire la différence ne produit jamais
     * [AccessibilityServiceState.PENDING].
     */
    fun read(): AccessibilityServiceState =
        if (isEnabled()) AccessibilityServiceState.ENABLED else AccessibilityServiceState.DISABLED
}

/**
 * État du service d'accessibilité de Niumi (étape 25) : la liste des services choisie par
 * l'utilisateur, lue dans les réglages système, croisée avec la liaison du service **dans ce
 * processus** ([AccessibilityServiceBinding], 2026-10-15).
 *
 * [PENDING] — inscrit dans `enabled_accessibility_services` mais aucune instance connectée dans ce
 * processus — est un état **transitoire** juste après le déverrouillage : Android relie les
 * services d'accessibilité après celui-ci, pas instantanément (mesuré le 2026-09-27 : l'état
 * existait 0,3 s après le déverrouillage et avait disparu 15 s plus tard ; liaison relevée 2,3 à
 * 3,6 s après). C'est aussi un état **durable** après une mort du processus, où Android ne relie
 * plus le service tant que l'utilisateur ne l'a pas réactivé (mesuré le 2026-09-25 sur HyperOS ;
 * règle AOSP des services plantés, SPEC_ANDROID §15). Seule la durée les distingue.
 */
enum class AccessibilityServiceState {
    ENABLED,
    PENDING,
    DISABLED,
    ;

    companion object {
        /** Règle pure, testable en JVM : `Settings.Secure` ne l'est pas. */
        fun of(
            listed: Boolean,
            bound: Boolean,
        ): AccessibilityServiceState =
            when {
                !listed -> DISABLED
                bound -> ENABLED
                else -> PENDING
            }
    }
}
