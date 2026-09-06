package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import com.niumi.database.BlockedPackage
import org.junit.Test

private const val SESSION_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"
private const val SELF_PACKAGE_NAME = "com.niumi.app"

/**
 * Algorithme de blocage (SPEC_ANDROID §12.2), sans dépendance Android : `BlockingDecision`
 * reçoit l'état de projection courant et le package au premier plan, et décide seul, sans
 * connaître `AccessibilityEvent`, `Context` ni `WindowManager`.
 */
class BlockingDecisionTest {
    private val niumi = BlockedPackage("com.exemple.jeu", "Jeu")
    private val autreApp = BlockedPackage("com.exemple.autre", "Autre application")

    @Test
    fun inactiveNeverBlocksAnything() {
        val action =
            BlockingDecision.decide(
                state = BlockedPackagesState.Inactive,
                foregroundPackage = niumi.packageName,
                selfPackageName = SELF_PACKAGE_NAME,
            )

        assertThat(action).isEqualTo(BlockAction.None)
    }

    @Test
    fun activeWithForegroundPackageNotListedDoesNothing() {
        val state = BlockedPackagesState.Active(SESSION_ID, setOf(niumi))

        val action =
            BlockingDecision.decide(
                state = state,
                foregroundPackage = autreApp.packageName,
                selfPackageName = SELF_PACKAGE_NAME,
            )

        assertThat(action).isEqualTo(BlockAction.None)
    }

    @Test
    fun activeWithForegroundPackageListedGoesHome() {
        val state = BlockedPackagesState.Active(SESSION_ID, setOf(niumi))

        val action =
            BlockingDecision.decide(
                state = state,
                foregroundPackage = niumi.packageName,
                selfPackageName = SELF_PACKAGE_NAME,
            )

        assertThat(action).isEqualTo(BlockAction.GoHome(niumi.packageName, niumi.displayNameSnapshot))
    }

    @Test
    fun releasingWithEmptyEffectivePackagesDoesNothing() {
        val state = BlockedPackagesState.Releasing(SESSION_ID, emptySet())

        val action =
            BlockingDecision.decide(
                state = state,
                foregroundPackage = niumi.packageName,
                selfPackageName = SELF_PACKAGE_NAME,
            )

        assertThat(action).isEqualTo(BlockAction.None)
    }

    @Test
    fun releasingWithNonEmptyEffectivePackagesStillBlocksListedPackage() {
        val state = BlockedPackagesState.Releasing(SESSION_ID, setOf(niumi))

        val action =
            BlockingDecision.decide(
                state = state,
                foregroundPackage = niumi.packageName,
                selfPackageName = SELF_PACKAGE_NAME,
            )

        assertThat(action).isEqualTo(BlockAction.GoHome(niumi.packageName, niumi.displayNameSnapshot))
    }

    @Test
    fun niumiItselfIsNeverBlockedEvenIfListed() {
        val niumiListedByMistake = BlockedPackage(SELF_PACKAGE_NAME, "Niumi")
        val state = BlockedPackagesState.Active(SESSION_ID, setOf(niumiListedByMistake))

        val action =
            BlockingDecision.decide(
                state = state,
                foregroundPackage = SELF_PACKAGE_NAME,
                selfPackageName = SELF_PACKAGE_NAME,
            )

        assertThat(action).isEqualTo(BlockAction.None)
    }
}
