package com.niumi.feature.session.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Fond de l'accueil (essai de Mehdi du 2026-10-30) : les mesures qui placent le dégradé, prises dans
 * la colonne qui défile, où la position d'une carte ne dépend pas du défilement. [scrollTop] : haut
 * de cette colonne dans l'écran ; [statusTop] et [rowsTop] : haut de la carte d'état et de la carte
 * des lignes dans la colonne.
 */
@Stable
internal class HomeBackdrop {
    var scrollTop by mutableStateOf<Float?>(null)
    var statusTop by mutableStateOf<Float?>(null)
    var rowsTop by mutableStateOf<Float?>(null)
}

/**
 * Blanc (`surface`) derrière le cadran, puis un dégradé lent vers le gris (`homeBackground`) : il part
 * du haut de la carte d'état, traverse la carte des jours et n'est gris plein qu'au haut de la carte
 * des lignes (précisions de Mehdi du 2026-10-30 : le gris commence au niveau de la carte d'état et
 * monte progressivement). Fixe, comme le cadran : seules les cartes défilent. Avant la première
 * mesure, des bornes estimées sous un cadran de la hauteur de maquette.
 */
@Composable
internal fun homeBackdropBrush(backdrop: HomeBackdrop): Brush {
    val palette = LocalNiumiPalette.current
    val density = LocalDensity.current
    val estimatedTop =
        with(density) {
            (NiumiDimens.screenTop + NiumiDimens.homeDialTop + NiumiDimens.dialHeight + NiumiDimens.homeCardTop).toPx()
        }
    val scrollTop = backdrop.scrollTop
    val statusTop = backdrop.statusTop
    val rowsTop = backdrop.rowsTop
    val top = if (scrollTop != null && statusTop != null) scrollTop + statusTop else estimatedTop
    val bottom =
        if (scrollTop != null && rowsTop != null) {
            scrollTop + rowsTop
        } else {
            top + with(density) { NiumiDimens.homeBackdropGradientEstimate.toPx() }
        }
    return Brush.verticalGradient(
        colors = listOf(palette.surface, palette.homeBackground),
        startY = top,
        endY = bottom,
    )
}

/**
 * Élargit l'élément de [overflow] de chaque côté, sur les marges de l'écran, sans changer la place
 * qu'il occupe dans la colonne. Sans carte autour du cadran (essai de Mehdi du 2026-10-30), sa zone
 * déborde de nouveau de 12 sur les marges, comme avant l'accueil en cartes : l'anneau, réduit pour
 * laisser tenir les étiquettes de côté, y gagne de la place.
 */
internal fun Modifier.overflowSideMargins(overflow: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = overflow.roundToPx()
        val width = constraints.maxWidth + 2 * extra
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
    }
