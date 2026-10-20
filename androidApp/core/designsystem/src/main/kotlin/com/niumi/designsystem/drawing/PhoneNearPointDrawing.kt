// `MagicNumber` supprimé pour tout le fichier : chaque nombre est une coordonnée ou une épaisseur
// du SVG de la maquette, recopiée telle quelle pour rester comparable à la source. Les nommer un à
// un n'apporterait que du bruit.
@file:Suppress("MagicNumber")

package com.niumi.designsystem.drawing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.component.NiumiPreviewFrame
import com.niumi.designsystem.ui.theme.LocalNiumiPalette

/**
 * Dessin du panneau 2 de l'écran 1 (maquette 02) : un téléphone approché d'un Niumi Point, trois
 * ondes entre les deux, le point ambre du Niumi Point comme seul détail ambre. Coordonnées du
 * `viewBox` 342 × 240 de la maquette. `SPECIFICATION.md` §4 en fait le même dessin que celui de
 * l'écran 10, dont la maquette 28 trace pourtant une variante (`viewBox` 342 × 220, trait 3,5) :
 * l'étape 38 tranchera entre une variante et un dessin unique.
 */
@Composable
fun PhoneNearPointDrawing(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    NiumiIllustration(contentDescription = contentDescription, modifier = modifier) {
        roundRect(36f, 10f, 120f, 220f, 22f, palette.background)
        roundRect(36f, 10f, 120f, 220f, 22f, palette.ink, strokeWidth = ILLUSTRATION_STROKE)
        line(at(84f, 28f), at(108f, 28f), palette.ink, ILLUSTRATION_STROKE)
        // Trois ondes, des courbes quadratiques symétriques autour de y = 120.
        WAVES.forEach { (x, top, reach) ->
            val start = at(x, top)
            val control = at(x + reach, WAVE_MIDDLE)
            val end = at(x, 2 * WAVE_MIDDLE - top)
            val wave =
                Path().apply {
                    moveTo(start.x, start.y)
                    quadraticTo(control.x, control.y, end.x, end.y)
                }
            drawScope.drawPath(wave, palette.ink, style = stroke(ILLUSTRATION_STROKE))
        }
        roundRect(222f, 62f, 116f, 116f, 26f, palette.ink, strokeWidth = ILLUSTRATION_STROKE)
        circle(310f, 90f, 8f, palette.amber)
    }
}

private const val WAVE_MIDDLE = 120f

/** Départ (x, y du haut) et portée de chaque onde : `M170 104 q12 16 0 32` et suivantes. */
private val WAVES = listOf(Triple(170f, 104f, 12f), Triple(183f, 90f, 22f), Triple(196f, 76f, 32f))

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun PhoneNearPointDrawingLightPreview() {
    NiumiPreviewFrame(dark = false) { PhoneNearPointDrawing(contentDescription = "Téléphone et Niumi Point") }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun PhoneNearPointDrawingDarkPreview() {
    NiumiPreviewFrame(dark = true) { PhoneNearPointDrawing(contentDescription = "Téléphone et Niumi Point") }
}
