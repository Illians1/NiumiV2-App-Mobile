// `MagicNumber` supprimé pour tout le fichier : chaque nombre est une coordonnée ou une épaisseur
// du SVG de la maquette, recopiée telle quelle pour rester comparable à la source. Les nommer un à
// un n'apporterait que du bruit.
@file:Suppress("MagicNumber")

package com.niumi.designsystem.drawing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.component.NiumiPreviewFrame
import com.niumi.designsystem.ui.theme.LocalNiumiPalette

/**
 * Dessin du panneau 1 de l'écran 1 (maquette 01) : un téléphone dont les six applications sont
 * grisées, un cadenas dans une pastille ambre, seul détail ambre du dessin (VALEURS-DE-DESIGN.md §2).
 * Coordonnées du `viewBox` 342 × 240 de la maquette.
 *
 * Les cases prennent la piste (`#E8E8E4` dans la maquette). Le cadenas est tracé à l'encre :
 * l'écran 1 n'existe qu'en clair, où l'encre est le `#0E0E10` de la maquette.
 */
@Composable
fun LockedPhoneDrawing(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    NiumiIllustration(contentDescription = contentDescription, modifier = modifier) {
        roundRect(111f, 10f, 120f, 220f, 22f, palette.background)
        roundRect(111f, 10f, 120f, 220f, 22f, palette.ink, strokeWidth = ILLUSTRATION_STROKE)
        line(at(159f, 28f), at(183f, 28f), palette.ink, ILLUSTRATION_STROKE)
        APP_CELLS.forEach { (x, y) -> roundRect(x, y, CELL, CELL, CELL_RADIUS, palette.track) }
        // Pastille de rayon 30 cernée d'un anneau de 4 couleur du fond, centré sur son bord : le
        // fond s'étend à 32, l'ambre s'arrête à 28.
        circle(231f, 196f, 32f, palette.background)
        circle(231f, 196f, 28f, palette.amber)
        val shackle =
            Path().apply {
                moveTo(at(222f, 194f).x, at(222f, 194f).y)
                lineTo(at(222f, 188f).x, at(222f, 188f).y)
                arcTo(Rect(at(222f, 179f), at(240f, 197f)), 180f, 180f, false)
                lineTo(at(240f, 194f).x, at(240f, 194f).y)
            }
        drawScope.drawPath(shackle, palette.ink, style = stroke(LOCK_STROKE))
        roundRect(218f, 194f, 26f, 18f, 4f, palette.ink, strokeWidth = LOCK_STROKE)
    }
}

private const val CELL = 34f
private const val CELL_RADIUS = 9f
private const val LOCK_STROKE = 2.6f

/** Coin haut gauche des six cases d'applications, deux colonnes de trois. */
private val APP_CELLS =
    listOf(129f to 52f, 179f to 52f, 129f to 98f, 179f to 98f, 129f to 144f, 179f to 144f)

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun LockedPhoneDrawingLightPreview() {
    NiumiPreviewFrame(dark = false) { LockedPhoneDrawing(contentDescription = "Téléphone verrouillé") }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun LockedPhoneDrawingDarkPreview() {
    NiumiPreviewFrame(dark = true) { LockedPhoneDrawing(contentDescription = "Téléphone verrouillé") }
}
