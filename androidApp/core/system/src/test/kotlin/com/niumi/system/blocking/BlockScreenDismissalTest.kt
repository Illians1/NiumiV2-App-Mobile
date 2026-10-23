package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Retrait de l'écran 7 quand une autre application passe au premier plan (décision de Mehdi du
 * 2026-10-22, étape 39) : un appel entrant ne doit pas rester caché sous l'écran de blocage. Le
 * lanceur, l'interface système et Niumi le gardent : le retour à l'accueil produit justement un
 * événement du lanceur, et l'écran 7 est lui-même une fenêtre de Niumi.
 */
class BlockScreenDismissalTest {
    private val home = setOf("com.miui.home", "com.android.launcher3")

    @Test
    fun theLauncherKeepsTheBlockScreen() {
        assertThat(dismisses("com.miui.home")).isFalse()
        assertThat(dismisses("com.android.launcher3")).isFalse()
    }

    @Test
    fun theSystemInterfaceKeepsTheBlockScreen() {
        assertThat(dismisses("com.android.systemui")).isFalse()
    }

    @Test
    fun niumiItselfKeepsTheBlockScreen() {
        assertThat(dismisses(NIUMI)).isFalse()
    }

    @Test
    fun anyOtherApplicationDismissesTheBlockScreen() {
        assertThat(dismisses("com.android.incallui")).isTrue()
        assertThat(dismisses("com.google.android.dialer")).isTrue()
        assertThat(dismisses("com.android.settings")).isTrue()
    }

    private fun dismisses(foreground: String): Boolean =
        BlockScreenDismissal.dismisses(foregroundPackage = foreground, selfPackageName = NIUMI, homePackages = home)

    private companion object {
        const val NIUMI = "com.niumi.app"
    }
}
