package com.niumi.feature.session.tabs

import androidx.compose.runtime.Composable
import com.niumi.designsystem.R
import com.niumi.designsystem.component.NiumiTabBar
import com.niumi.designsystem.component.NiumiTabBarItem
import com.niumi.designsystem.component.NiumiTabIcon

/** Les quatre onglets de la barre du bas, dans l'ordre de la maquette 16. */
enum class MainTab { WAKE, BLOCKING_ONLY, TRACKING, SETTINGS }

/**
 * Textes de la barre d'onglets (maquette 16 du 2026-10-25, décision de Mehdi), recopiés mot pour
 * mot ; les libellés lus par TalkBack sont ceux de la source de la maquette (`aria-label`).
 */
object MainTabTexts {
    const val WAKE = "Réveil"
    const val BLOCKING_ONLY = "Blocage seul"
    const val TRACKING = "Suivi"
    const val SETTINGS = "Réglages"
    const val SOON = "Bientôt"

    fun soon(label: String): String = "$label, bientôt disponible"
}

/**
 * Les onglets, dans l'ordre de [MainTab] : « Réveil » et « Réglages » disponibles ; « Blocage seul »
 * et « Suivi » portent « Bientôt », sans action, tant que leurs pages n'existent pas (D24 de
 * `POINTS-OUVERTS.md`).
 */
fun mainTabItems(): List<NiumiTabBarItem> =
    MainTab.entries.map { tab ->
        when (tab) {
            MainTab.WAKE -> {
                NiumiTabBarItem(MainTabTexts.WAKE, NiumiTabIcon.Clock)
            }

            MainTab.BLOCKING_ONLY -> {
                NiumiTabBarItem(
                    label = MainTabTexts.BLOCKING_ONLY,
                    icon = NiumiTabIcon.Vector(R.drawable.ic_tab_shield),
                    badge = MainTabTexts.SOON,
                    accessibilityLabel = MainTabTexts.soon(MainTabTexts.BLOCKING_ONLY),
                )
            }

            MainTab.TRACKING -> {
                NiumiTabBarItem(
                    label = MainTabTexts.TRACKING,
                    icon = NiumiTabIcon.Vector(R.drawable.ic_tab_tracking),
                    badge = MainTabTexts.SOON,
                    accessibilityLabel = MainTabTexts.soon(MainTabTexts.TRACKING),
                )
            }

            MainTab.SETTINGS -> {
                NiumiTabBarItem(
                    label = MainTabTexts.SETTINGS,
                    icon =
                        NiumiTabIcon.Vector(
                            R.drawable.ic_tab_settings,
                            selectedId = R.drawable.ic_tab_settings_filled,
                        ),
                )
            }
        }
    }

/**
 * Ce que fait le toucher d'un onglet depuis l'écran de [selected] : rien sur l'onglet déjà choisi,
 * [onWake] ou [onSettings] sinon. Les onglets « Bientôt » n'arrivent jamais ici ([NiumiTabBar]).
 */
internal fun tabAction(
    touched: MainTab,
    selected: MainTab,
    onWake: () -> Unit,
    onSettings: () -> Unit,
): (() -> Unit)? =
    when {
        touched == selected -> null
        touched == MainTab.WAKE -> onWake
        touched == MainTab.SETTINGS -> onSettings
        else -> null
    }

/**
 * Barre d'onglets de l'accueil et des réglages (décision de Mehdi du 2026-10-25) : [selected] est
 * l'onglet de l'écran qui la pose. Elle reste utilisable pendant une session, sombre comme l'écran.
 */
@Composable
fun MainTabBar(
    selected: MainTab,
    onWake: () -> Unit,
    onSettings: () -> Unit,
) {
    NiumiTabBar(
        items = mainTabItems(),
        selectedIndex = selected.ordinal,
        onSelect = { index -> tabAction(MainTab.entries[index], selected, onWake, onSettings)?.invoke() },
    )
}
