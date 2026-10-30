package com.niumi.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Marges des maquettes posées sur les barres système (VALEURS-DE-DESIGN.md §4), vérifiées par
 * `NiumiMarginsTest` :
 * - côtés et haut : la plus grande de la marge de la maquette et de la zone système, jamais leur
 *   somme (décision de Mehdi du 2026-10-20). Les 56 du haut de la maquette iPhone couvrent déjà la
 *   barre d'état ;
 * - bas : la zone système plus [NiumiDimens.systemBarGap], sans descendre sous la marge de la
 *   maquette (décision de Mehdi du 2026-10-22, étape 38). Avec la règle précédente, la barre à
 *   trois boutons (48) remplaçait les 34 de la maquette, et le bouton du bas la touchait ; la somme
 *   entière laissait jusqu'à 80 dp de vide.
 */
internal fun marginsOverSystemBars(
    system: PaddingValues,
    layoutDirection: LayoutDirection,
    margins: PaddingValues,
): PaddingValues =
    PaddingValues(
        start = max(margins.calculateStartPadding(layoutDirection), system.calculateStartPadding(layoutDirection)),
        top = max(margins.calculateTopPadding(), system.calculateTopPadding()),
        end = max(margins.calculateEndPadding(layoutDirection), system.calculateEndPadding(layoutDirection)),
        bottom = max(margins.calculateBottomPadding(), system.calculateBottomPadding() + NiumiDimens.systemBarGap),
    )

/**
 * Marges d'un écran dont le bas est pris par la barre d'onglets (décision de Mehdi du 2026-10-25) :
 * côtés et haut comme [marginsOverSystemBars], rien en bas. La barre couvre la barre système de son
 * fond et le contenu s'arrête contre elle, comme dans la maquette 16. Sans [keepTop], rien en haut
 * non plus : le contenu défile sous la bande du haut (`niumiTopFade`).
 */
internal fun marginsAboveTabBar(
    system: PaddingValues,
    layoutDirection: LayoutDirection,
    margins: PaddingValues,
    keepTop: Boolean = true,
): PaddingValues {
    val over = marginsOverSystemBars(system, layoutDirection, margins)
    return PaddingValues(
        start = over.calculateStartPadding(layoutDirection),
        top = if (keepTop) over.calculateTopPadding() else 0.dp,
        end = over.calculateEndPadding(layoutDirection),
        bottom = 0.dp,
    )
}

/**
 * [marginsOverSystemBars] appliqué à un élément bord à bord, d'après les retraits [insets] de la
 * fenêtre ; sans [keepTop], rien en haut (contenu qui défile sous la bande du haut).
 */
@Composable
internal fun Modifier.marginsOver(
    insets: WindowInsets,
    margins: PaddingValues,
    keepTop: Boolean = true,
): Modifier {
    val over = marginsOverSystemBars(insets.asPaddingValues(), LocalLayoutDirection.current, margins)
    val direction = LocalLayoutDirection.current
    return padding(
        PaddingValues(
            start = over.calculateStartPadding(direction),
            top = if (keepTop) over.calculateTopPadding() else 0.dp,
            end = over.calculateEndPadding(direction),
            bottom = over.calculateBottomPadding(),
        ),
    )
}

/** [marginsAboveTabBar] appliqué à un élément bord à bord, d'après les retraits [insets] de la fenêtre. */
@Composable
internal fun Modifier.marginsAboveTabBar(
    insets: WindowInsets,
    margins: PaddingValues,
    keepTop: Boolean = true,
): Modifier = padding(marginsAboveTabBar(insets.asPaddingValues(), LocalLayoutDirection.current, margins, keepTop))
