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
 * Marges d'écran sur Android (VALEURS-DE-DESIGN.md §4). Côtés et haut : la plus grande de la marge
 * de la maquette et de la zone système (décision de Mehdi du 2026-10-20) ; les 56 du haut couvrent
 * la barre d'état. Bas, depuis le 2026-10-22 (décision de Mehdi, étape 38) : la barre de navigation
 * plus un espace de [NiumiDimens.systemBarGap], sans descendre sous les 34 de la maquette ; le
 * bouton ne touche plus la barre à trois boutons.
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
    fun aThinOrAbsentNavigationBarLeavesTheMarginOfTheDesign() {
        assertThat(bottom(systemBottom = 16)).isEqualTo(34.dp)
        assertThat(bottom(systemBottom = 0)).isEqualTo(34.dp)
    }

    /** Trois boutons (48) : 16 d'espace au-dessus ; gestes (24) : 40, un peu plus que la maquette. */
    @Test
    fun aNavigationBarKeepsAGapAboveIt() {
        assertThat(NiumiDimens.systemBarGap).isEqualTo(16.dp)
        assertThat(bottom(systemBottom = 48)).isEqualTo(64.dp)
        assertThat(bottom(systemBottom = 24)).isEqualTo(40.dp)
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

    /** Barre d'onglets en bas (décision du 2026-10-25) : elle prend le bas, côtés et haut inchangés. */
    @Test
    fun aTabBarTakesTheBottomAndLeavesTheSidesAndTopAsTheyAre() {
        val system = PaddingValues(start = 30.dp, top = 40.dp, bottom = 48.dp)

        val margins = marginsAboveTabBar(system, LayoutDirection.Ltr, screen)

        assertThat(margins.calculateBottomPadding()).isEqualTo(0.dp)
        assertThat(margins.calculateTopPadding()).isEqualTo(56.dp)
        assertThat(margins.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(30.dp)
        assertThat(margins.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(24.dp)
    }

    /** Contenu qui défile sous la bande du haut (accueil, 2026-10-30) : plus rien en haut, le reste inchangé. */
    @Test
    fun contentUnderTheTopBandHasNoTopMargin() {
        val system = PaddingValues(start = 30.dp, top = 40.dp, bottom = 48.dp)

        val margins = marginsAboveTabBar(system, LayoutDirection.Ltr, screen, keepTop = false)

        assertThat(margins.calculateTopPadding()).isEqualTo(0.dp)
        assertThat(margins.calculateBottomPadding()).isEqualTo(0.dp)
        assertThat(margins.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(30.dp)
        assertThat(margins.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(24.dp)
    }

    @Test
    fun theScreenMarginsAreThoseOfTheDesign() {
        assertThat(screen.calculateTopPadding()).isEqualTo(NiumiDimens.screenTop)
        assertThat(screen.calculateBottomPadding()).isEqualTo(NiumiDimens.screenBottom)
        assertThat(screen.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(NiumiDimens.screenHorizontal)
        assertThat(screen.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(NiumiDimens.screenHorizontal)
    }
}
