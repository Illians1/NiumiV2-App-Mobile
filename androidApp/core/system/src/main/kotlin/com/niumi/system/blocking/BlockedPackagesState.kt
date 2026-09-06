package com.niumi.system.blocking

import com.niumi.database.BlockedPackage

/**
 * Projection locale de la liste de blocage d'une session, lue par
 * [NiumiBlockingAccessibilityService][com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService]
 * (SPEC_ANDROID §12.2). `Releasing` porte la liste *effective* restant à débloquer : le
 * coordinateur (étape 15) peut y retirer des packages au fur et à mesure des effets
 * `REMOVE_BLOCKING` exécutés, sans attendre que la libération soit totale.
 */
sealed interface BlockedPackagesState {
    data object Inactive : BlockedPackagesState

    data class Active(
        val sessionId: String,
        val packages: Set<BlockedPackage>,
    ) : BlockedPackagesState

    data class Releasing(
        val sessionId: String,
        val effectivePackages: Set<BlockedPackage>,
    ) : BlockedPackagesState
}
