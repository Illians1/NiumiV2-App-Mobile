package com.niumi.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiDimens
import org.junit.Test

/**
 * Marges d'écran sur Android (décision de Mehdi du 2026-10-20, VALEURS-DE-DESIGN.md §4) : de
 * chaque côté, la plus grande de la marge de la maquette et de la zone système (barre d'état,
 * barre de navigation, encoche, clavier), jamais leur somme. Les 34 du bas et les 56 du haut des
 * maquettes iPhone couvrent déjà la barre d'accueil et la barre d'état.
 */
class NiumiMarginsTest {
    private val screen = NiumiDimens.screenMargins

    private fun bottom(systemBottom: Int) =
        marginsOverSystemBars(
            PaddingValues(bottom = systemBottom.dp),
            LayoutDirection.Ltr,
            screen,
        ).calculateBottomPadding()

    private fun top(systemTop: Int) =
        marginsOverSystemBars(PaddingValues(top = systemTop.dp), LayoutDirection.Ltr, screen).calculateTopPadding()

    @Test
    fun aThinNavigationBarLeavesTheMarginOfTheDesign() {
        assertThat(bottom(systemBottom = 16)).isEqualTo(34.dp)
        assertThat(bottom(systemBottom = 0)).isEqualTo(34.dp)
    }

    @Test
    fun aTallNavigationBarReplacesTheMarginInsteadOfAddingToIt() {
        assertThat(bottom(systemBottom = 48)).isEqualTo(48.dp)
    }

    @Test
    fun theTopMarginCoversTheStatusBar() {
        assertThat(top(systemTop = 40)).isEqualTo(56.dp)
        assertThat(top(systemTop = 60)).isEqualTo(60.dp)
    }

    @Test
    fun sidesFollowTheLayoutDirection() {
        val system = PaddingValues(start = 30.dp, end = 0.dp)

        val ltr = marginsOverSystemBars(system, LayoutDirection.Ltr, screen)
        val rtl = marginsOverSystemBars(system, LayoutDirection.Rtl, screen)

        assertThat(ltr.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(30.dp)
        assertThat(ltr.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(24.dp)
        assertThat(rtl.calculateRightPadding(LayoutDirection.Rtl)).isEqualTo(30.dp)
        assertThat(rtl.calculateLeftPadding(LayoutDirection.Rtl)).isEqualTo(24.dp)
    }

    @Test
    fun theScreenMarginsAreThoseOfTheDesign() {
        assertThat(screen.calculateTopPadding()).isEqualTo(NiumiDimens.screenTop)
        assertThat(screen.calculateBottomPadding()).isEqualTo(NiumiDimens.screenBottom)
        assertThat(screen.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(NiumiDimens.screenHorizontal)
        assertThat(screen.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(NiumiDimens.screenHorizontal)
    }
}
