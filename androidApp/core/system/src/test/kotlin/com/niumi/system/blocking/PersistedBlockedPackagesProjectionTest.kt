package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import com.niumi.database.BlockedPackage
import com.niumi.database.blocking.BlockedPackagesRead
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.system.blocking.fakes.RecordingBlockedPackagesSource
import com.niumi.system.common.OperationResult
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val SESSION_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"
private const val OTHER_SESSION_ID = "9a0b1c2d-3e4f-5a6b-7c8d-9e0f1a2b3c4d"

/**
 * Cache de la projection de blocage (SPEC_ANDROID §12.2). Deux propriétés portent l'étape 15 :
 * [refresh] fait de la persistance l'autorité, et une persistance illisible ne lève **jamais** le
 * blocage (§13). Les écritures optimistes du contrôleur restent testées telles quelles : elles
 * n'ont pas changé de contrat depuis l'étape 5.
 */
class PersistedBlockedPackagesProjectionTest {
    private val packages = setOf(BlockedPackage("com.exemple.jeu", "Jeu"))
    private val source = RecordingBlockedPackagesSource()
    private val projection = PersistedBlockedPackagesProjection(source)

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
    fun removingAnotherSessionLeavesTheProjectionUntouched() {
        projection.apply(SESSION_ID, packages)

        val result = projection.remove(OTHER_SESSION_ID)

        assertThat(result).isEqualTo(OperationResult.AlreadySatisfied)
        assertThat(projection.current()).isEqualTo(BlockedPackagesState.Active(SESSION_ID, packages))
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

    @Test
    fun effectivePackagesReflectsWhatIsLeftWhileReleasing() {
        source.next = BlockedPackagesRead.Resolved(BlockedPackagesState.Releasing(SESSION_ID, packages))

        runTest { projection.refresh() }

        assertThat(projection.effectivePackages()).containsExactly("com.exemple.jeu")
    }

    /** Le cache n'est pas une source de vérité : la persistance le remplace, même à contre-pied. */
    @Test
    fun refreshAdoptsWhatThePersistenceSays() =
        runTest {
            projection.apply(SESSION_ID, packages)
            source.next = BlockedPackagesRead.Resolved(BlockedPackagesState.Inactive)

            val refreshed = projection.refresh()

            assertThat(refreshed).isEqualTo(BlockedPackagesState.Inactive)
            assertThat(projection.current()).isEqualTo(BlockedPackagesState.Inactive)
        }

    @Test
    fun refreshRebuildsAnActiveSessionAfterAProcessRestart() =
        runTest {
            source.next = BlockedPackagesRead.Resolved(BlockedPackagesState.Active(SESSION_ID, packages))

            projection.refresh()

            assertThat(projection.current()).isEqualTo(BlockedPackagesState.Active(SESSION_ID, packages))
        }

    /** SPEC_ANDROID §13 : aucune suppression silencieuse du blocage sur snapshot illisible. */
    @Test
    fun anUnreadablePersistenceNeverClearsTheBlocking() =
        runTest {
            projection.apply(SESSION_ID, packages)
            source.next = BlockedPackagesRead.Unreadable("json")

            val refreshed = projection.refresh()

            assertThat(refreshed).isEqualTo(BlockedPackagesState.Active(SESSION_ID, packages))
            assertThat(projection.current()).isEqualTo(BlockedPackagesState.Active(SESSION_ID, packages))
        }

    @Test
    fun anUnreadablePersistenceOnAnEmptyCacheStaysInactive() =
        runTest {
            source.next = BlockedPackagesRead.Unreadable("absent")

            assertThat(projection.refresh()).isEqualTo(BlockedPackagesState.Inactive)
        }
}
