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

/** [marginsOverSystemBars] appliqué à un élément bord à bord, d'après les retraits [insets] de la fenêtre. */
@Composable
internal fun Modifier.marginsOver(
    insets: WindowInsets,
    margins: PaddingValues,
): Modifier = padding(marginsOverSystemBars(insets.asPaddingValues(), LocalLayoutDirection.current, margins))
