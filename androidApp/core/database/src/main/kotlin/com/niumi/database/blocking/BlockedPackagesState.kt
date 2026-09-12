package com.niumi.database.blocking

import com.niumi.database.BlockedPackage

/**
 * Projection locale de la liste de blocage d'une session, lue par
 * `NiumiBlockingAccessibilityService` (SPEC_ANDROID §12.2). `Releasing` porte la liste
 * *effective* restant à débloquer : pendant `RELEASING`, le blocage dépend des effets de
 * libération déjà réussis (SPEC_ANDROID §3), pas du seul état de la session.
 *
 * Vit dans `:core:database` depuis l'étape 15 : la projection est désormais reconstruite depuis
 * Room ou depuis le snapshot Direct Boot, et [RoomBlockedPackagesSource] doit pouvoir en
 * fabriquer une. `:core:database` ne peut pas dépendre de `:core:system` (règle de dépendance
 * SPEC_ANDROID §6), donc c'est le type qui descend — il importait déjà [BlockedPackage] d'ici.
 * Même motif que `DirectBootWriteResult` à l'étape 10. L'interface de lecture
 * `BlockedPackagesProjection` reste dans `:core:system`, avec ses consommateurs.
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
