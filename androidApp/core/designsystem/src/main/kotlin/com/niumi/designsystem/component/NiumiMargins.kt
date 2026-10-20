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

/**
 * Marges des maquettes posées sur les barres système (décision de Mehdi du 2026-10-20,
 * VALEURS-DE-DESIGN.md §4) : de chaque côté, la plus grande de la marge de la maquette et de la
 * zone système, jamais leur somme. Sur la maquette iPhone, les 56 du haut couvrent déjà la barre
 * d'état et les 34 du bas la barre d'accueil ; les additionner aux barres d'Android laissait
 * jusqu'à 80 dp de vide sous le bouton principal. Vérifié par `NiumiMarginsTest`.
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
        bottom = max(margins.calculateBottomPadding(), system.calculateBottomPadding()),
    )

/** [marginsOverSystemBars] appliqué à un élément bord à bord, d'après les retraits [insets] de la fenêtre. */
@Composable
internal fun Modifier.marginsOver(
    insets: WindowInsets,
    margins: PaddingValues,
): Modifier = padding(marginsOverSystemBars(insets.asPaddingValues(), LocalLayoutDirection.current, margins))
