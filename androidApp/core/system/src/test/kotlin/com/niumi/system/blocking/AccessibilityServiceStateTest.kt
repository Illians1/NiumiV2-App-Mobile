package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Étape 25 : la liste des services et la liaison du service, lues séparément, donnent trois états
 * et non deux. Depuis le 2026-10-15, la liaison est celle de l'instance dans ce processus, plus
 * l'interrupteur global `accessibility_enabled`.
 */
class AccessibilityServiceStateTest {
    @Test
    fun listedAndBoundIsEnabled() {
        assertThat(AccessibilityServiceState.of(listed = true, bound = true))
            .isEqualTo(AccessibilityServiceState.ENABLED)
    }

    /**
     * L'état mesuré 0,3 s après le déverrouillage le 2026-09-27, et après une mort du processus.
     * Le 2026-10-15, « Brick App Blocker » tenait `accessibility_enabled` à 1 pendant que Niumi
     * était délié : l'ancienne règle lisait `ENABLED`, et l'incident n'était jamais consigné.
     */
    @Test
    fun listedButNotBoundInThisProcessIsPendingEvenIfAnotherServiceKeepsTheGlobalSwitchOn() {
        assertThat(AccessibilityServiceState.of(listed = true, bound = false))
            .isEqualTo(AccessibilityServiceState.PENDING)
    }

    /** Retiré de la liste : le choix de l'utilisateur, quelle que soit la liaison. */
    @Test
    fun notListedIsDisabledWhateverTheBinding() {
        assertThat(AccessibilityServiceState.of(listed = false, bound = true))
            .isEqualTo(AccessibilityServiceState.DISABLED)
        assertThat(AccessibilityServiceState.of(listed = false, bound = false))
            .isEqualTo(AccessibilityServiceState.DISABLED)
    }
}
