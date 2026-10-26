// `MagicNumber` supprimé pour tout le fichier : chaque nombre est une coordonnée de la maquette 42,
// recopiée telle quelle pour rester comparable à la source.
@file:Suppress("MagicNumber")

package com.niumi.designsystem.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.component.NiumiPreviewFrame
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiPalettes

/**
 * Soleil levant de l'écran 11 (maquette 42) : un disque de 144 dont seuls les 84 du haut dépassent
 * de l'horizon, en dégradé ambre de haut en bas ([NiumiPalettes.SunGradient], le seul dégradé des
 * écrans clairs, VALEURS-DE-DESIGN.md §2 ; gardé « pour l'instant », D15), posé sur une ligne
 * d'horizon de 294 × 3 aux bouts arrondis, à l'encre.
 *
 * Coordonnées d'un repère de 342 × 85, la largeur utile de l'écran : le disque de la maquette
 * commence à 99 de la marge, l'horizon à 24. Décoratif (`aria-hidden` dans la maquette) : aucun
 * nœud d'accessibilité.
 */
@Composable
fun SunriseDrawing(modifier: Modifier = Modifier) {
    val palette = LocalNiumiPalette.current
    Canvas(
        modifier =
            modifier
                .widthIn(max = NiumiDimens.illustrationWidth)
                .fillMaxWidth()
                .aspectRatio(NiumiDimens.illustrationWidth / NiumiDimens.sunriseHeight),
    ) {
        val scope = IllustrationScope(this, size.width / VIEW_BOX_WIDTH)
        val sunTop = scope.at(SUN_LEFT, 0f)
        val sunDiameter = SUN_DIAMETER * size.width / VIEW_BOX_WIDTH
        clipRect(right = size.width, bottom = scope.at(0f, SUN_VISIBLE_HEIGHT).y) {
            drawOval(
                brush =
                    Brush.verticalGradient(
                        colorStops = NiumiPalettes.SunGradient.toTypedArray(),
                        startY = sunTop.y,
                        endY = sunTop.y + sunDiameter,
                    ),
                topLeft = Offset(sunTop.x, sunTop.y),
                size = Size(sunDiameter, sunDiameter),
            )
        }
        scope.roundRect(HORIZON_LEFT, HORIZON_TOP, HORIZON_WIDTH, HORIZON_HEIGHT, HORIZON_HEIGHT / 2, palette.ink)
    }
}

private const val SUN_LEFT = 99f
private const val SUN_DIAMETER = 144f
private const val SUN_VISIBLE_HEIGHT = 84f
private const val HORIZON_LEFT = 24f
private const val HORIZON_TOP = 82f
private const val HORIZON_WIDTH = 294f
private const val HORIZON_HEIGHT = 3f

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun SunriseDrawingPreview() {
    NiumiPreviewFrame(dark = false) { SunriseDrawing() }
}
