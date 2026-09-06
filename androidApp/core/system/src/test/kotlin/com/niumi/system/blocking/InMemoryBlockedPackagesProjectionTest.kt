package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import com.niumi.database.BlockedPackage
import com.niumi.system.common.OperationResult
import org.junit.Test

private const val SESSION_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"

/**
 * Projection en mémoire de la liste de blocage active, partagée par le contrôleur et le
 * service d'accessibilité au sein d'un même process (SPEC_ANDROID §12.2). Implémentation
 * provisoire, remplacée par une lecture Room à l'étape 15 sans changer l'interface — même
 * motif que `InMemoryTechnicalEventLog`.
 */
class InMemoryBlockedPackagesProjectionTest {
    private val packages = setOf(BlockedPackage("com.exemple.jeu", "Jeu"))
    private val projection = InMemoryBlockedPackagesProjection()

    @Test
    fun startsInactive() {
        assertThat(projection.current()).isEqualTo(BlockedPackagesState.Inactive)
    }

    @Test
    fun applyMakesTheSessionActive() {
        val result = projection.apply(SESSION_ID, packages)

        assertThat(result).isEqualTo(OperationResult.Success)
        assertThat(projection.current()).isEqualTo(BlockedPackagesState.Active(SESSION_ID, packages))
    }

    @Test
    fun applyingTheSameSessionTwiceIsAlreadySatisfied() {
        projection.apply(SESSION_ID, packages)

        val result = projection.apply(SESSION_ID, packages)

        assertThat(result).isEqualTo(OperationResult.AlreadySatisfied)
    }

    @Test
    fun removingAnUnknownSessionIsAlreadySatisfied() {
        val result = projection.remove(SESSION_ID)

        assertThat(result).isEqualTo(OperationResult.AlreadySatisfied)
    }

    @Test
    fun removeClearsTheProjection() {
        projection.apply(SESSION_ID, packages)

        val result = projection.remove(SESSION_ID)

        assertThat(result).isEqualTo(OperationResult.Success)
        assertThat(projection.current()).isEqualTo(BlockedPackagesState.Inactive)
    }

    @Test
    fun effectivePackagesReflectsTheActivePackageNames() {
        projection.apply(SESSION_ID, packages)

        assertThat(projection.effectivePackages()).containsExactly("com.exemple.jeu")
    }

    @Test
    fun effectivePackagesIsEmptyWhenInactive() {
        assertThat(projection.effectivePackages()).isEmpty()
    }
}
