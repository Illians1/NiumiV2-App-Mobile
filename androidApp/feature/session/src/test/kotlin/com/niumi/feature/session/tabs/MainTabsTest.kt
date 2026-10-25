package com.niumi.feature.session.tabs

import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.R
import com.niumi.designsystem.component.NiumiTabIcon
import org.junit.Test

/**
 * Barre d'onglets du bas (maquette 16, décision de Mehdi du 2026-10-25) : quatre onglets dans
 * l'ordre, textes mot pour mot, « Blocage seul » et « Suivi » annoncés « Bientôt » et sans action.
 */
class MainTabsTest {
    private val items = mainTabItems()

    @Test
    fun theFourTabsAreInTheOrderOfTheDesignWithTheirTexts() {
        assertThat(items.map { it.label }).containsExactly("Réveil", "Blocage seul", "Suivi", "Réglages").inOrder()
        assertThat(items.map { it.accessibilityLabel })
            .containsExactly("Réveil", "Blocage seul, bientôt disponible", "Suivi, bientôt disponible", "Réglages")
            .inOrder()
        assertThat(MainTabTexts.SOON).isEqualTo("Bientôt")
    }

    @Test
    fun onlyBlockingOnlyAndTrackingAreComingSoon() {
        assertThat(items.map { it.badge }).containsExactly(null, "Bientôt", "Bientôt", null).inOrder()
        assertThat(items.map { it.available }).containsExactly(true, false, false, true).inOrder()
    }

    @Test
    fun eachTabHasTheIconOfTheDesign() {
        assertThat(items.map { it.icon })
            .containsExactly(
                NiumiTabIcon.Clock,
                NiumiTabIcon.Vector(R.drawable.ic_tab_shield),
                NiumiTabIcon.Vector(R.drawable.ic_tab_tracking),
                NiumiTabIcon.Vector(R.drawable.ic_tab_settings, selectedId = R.drawable.ic_tab_settings_filled),
            ).inOrder()
    }

    @Test
    fun touchingAnotherAvailableTabOpensItsScreen() {
        val calls = mutableListOf<String>()
        val wake = { calls += "wake" }
        val settings = { calls += "settings" }

        tabAction(MainTab.SETTINGS, selected = MainTab.WAKE, onWake = { wake() }, onSettings = { settings() })?.invoke()
        tabAction(MainTab.WAKE, selected = MainTab.SETTINGS, onWake = { wake() }, onSettings = { settings() })?.invoke()

        assertThat(calls).containsExactly("settings", "wake").inOrder()
    }

    @Test
    fun touchingTheSelectedTabOrAComingSoonTabDoesNothing() {
        MainTab.entries.forEach { selected ->
            assertThat(tabAction(selected, selected, onWake = {}, onSettings = {})).isNull()
            assertThat(tabAction(MainTab.BLOCKING_ONLY, selected, onWake = {}, onSettings = {})).isNull()
            assertThat(tabAction(MainTab.TRACKING, selected, onWake = {}, onSettings = {})).isNull()
        }
    }
}
