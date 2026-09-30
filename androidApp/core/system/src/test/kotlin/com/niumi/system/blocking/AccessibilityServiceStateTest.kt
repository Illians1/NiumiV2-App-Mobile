package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Étape 25 : les deux réglages système, lus séparément, donnent trois états et non deux. */
class AccessibilityServiceStateTest {
    @Test
    fun listedAndGloballyEnabledIsEnabled() {
        assertThat(AccessibilityServiceState.of(globallyEnabled = true, listed = true))
            .isEqualTo(AccessibilityServiceState.ENABLED)
    }

    /** L'état mesuré 0,3 s après le déverrouillage le 2026-09-27, et après une mort du processus. */
    @Test
    fun listedButNotBoundIsPending() {
        assertThat(AccessibilityServiceState.of(globallyEnabled = false, listed = true))
            .isEqualTo(AccessibilityServiceState.PENDING)
    }

    /** Retiré de la liste : le choix de l'utilisateur, quel que soit l'interrupteur global. */
    @Test
    fun notListedIsDisabledWhateverTheGlobalSwitch() {
        assertThat(AccessibilityServiceState.of(globallyEnabled = true, listed = false))
            .isEqualTo(AccessibilityServiceState.DISABLED)
        assertThat(AccessibilityServiceState.of(globallyEnabled = false, listed = false))
            .isEqualTo(AccessibilityServiceState.DISABLED)
    }
}
