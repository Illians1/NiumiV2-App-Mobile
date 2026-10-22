// `MagicNumber` supprimé pour tout le fichier : chaque nombre est une coordonnée ou une épaisseur
// du SVG de la maquette, recopiée telle quelle pour rester comparable à la source. Les nommer un à
// un n'apporterait que du bruit.
@file:Suppress("MagicNumber")

package com.niumi.designsystem.drawing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.component.NiumiPreviewFrame
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Un téléphone approché d'un Niumi Point, trois ondes entre les deux, le point ambre du Niumi Point
 * comme seul détail ambre. Deux tracés, chacun recopié de sa maquette (étape 38 : une variante
 * plutôt qu'un dessin unique, pour rester fidèle aux deux sources) :
 * - [PhoneNearPointVariant.Presentation] : panneau 2 de l'écran 1 (maquette 02), `viewBox`
 *   342 × 240, trait 3, affiché 342 × 240 au plus ;
 * - [PhoneNearPointVariant.Alarm] : écran 10 (maquette 28), `viewBox` 342 × 220, trait 3,5,
 *   affiché 264 × 170 au plus.
 */
@Composable
fun PhoneNearPointDrawing(
    contentDescription: String,
    modifier: Modifier = Modifier,
    variant: PhoneNearPointVariant = PhoneNearPointVariant.Presentation,
) {
    val palette = LocalNiumiPalette.current
    val g = variant.geometry
    NiumiIllustration(
        contentDescription = contentDescription,
        modifier = modifier,
        maxWidth = variant.maxWidth,
        maxHeight = variant.maxHeight,
    ) {
        val phone = g.phone
        roundRect(phone.x, phone.y, phone.width, phone.height, phone.radius, palette.background)
        roundRect(phone.x, phone.y, phone.width, phone.height, phone.radius, palette.ink, strokeWidth = g.stroke)
        line(at(g.speakerX, g.speakerY), at(g.speakerX + SPEAKER_LENGTH, g.speakerY), palette.ink, g.stroke)
        // Trois ondes, des courbes quadratiques symétriques autour de `waveMiddle`.
        g.waves.forEach { (x, top, reach) ->
            val start = at(x, top)
            val control = at(x + reach, g.waveMiddle)
            val end = at(x, 2 * g.waveMiddle - top)
            val wave =
                Path().apply {
                    moveTo(start.x, start.y)
                    quadraticTo(control.x, control.y, end.x, end.y)
                }
            drawScope.drawPath(wave, palette.ink, style = stroke(g.stroke))
        }
        val point = g.point
        roundRect(point.x, point.y, point.width, point.height, point.radius, palette.ink, strokeWidth = g.stroke)
        circle(g.dotX, g.dotY, DOT_RADIUS, palette.amber)
    }
}

/** Les deux tracés du dessin, l'un par maquette. */
enum class PhoneNearPointVariant(
    internal val geometry: PhoneNearPointGeometry,
    internal val maxWidth: Dp,
    internal val maxHeight: Dp,
) {
    /** Maquette 02 : `M170 104 q12 16 0 32` et suivantes. */
    Presentation(
        PhoneNearPointGeometry(
            stroke = ILLUSTRATION_STROKE,
            phone = SvgRect(36f, 10f, 120f, 220f, 22f),
            speakerX = 84f,
            speakerY = 28f,
            waveMiddle = 120f,
            waves = listOf(Triple(170f, 104f, 12f), Triple(183f, 90f, 22f), Triple(196f, 76f, 32f)),
            point = SvgRect(222f, 62f, 116f, 116f, 26f),
            dotX = 310f,
            dotY = 90f,
        ),
        NiumiDimens.illustrationWidth,
        NiumiDimens.illustrationHeight,
    ),

    /** Maquette 28 : `M156 96 q10 14 0 28` et suivantes. */
    Alarm(
        PhoneNearPointGeometry(
            stroke = 3.5f,
            phone = SvgRect(46f, 20f, 96f, 180f, 18f),
            speakerX = 82f,
            speakerY = 36f,
            waveMiddle = 110f,
            waves = listOf(Triple(156f, 96f, 10f), Triple(168f, 84f, 20f), Triple(180f, 72f, 30f)),
            point = SvgRect(212f, 50f, 120f, 120f, 26f),
            dotX = 304f,
            dotY = 78f,
        ),
        NiumiDimens.alarmDrawingWidth,
        NiumiDimens.alarmDrawingHeight,
    ),
}

/** Coordonnées d'un tracé, dans le `viewBox` de sa maquette. */
internal data class PhoneNearPointGeometry(
    val stroke: Float,
    val phone: SvgRect,
    val speakerX: Float,
    val speakerY: Float,
    val waveMiddle: Float,
    /** Départ (x, y du haut) et portée de chaque onde. */
    val waves: List<Triple<Float, Float, Float>>,
    val point: SvgRect,
    val dotX: Float,
    val dotY: Float,
)

/** Un `<rect>` arrondi du SVG. */
internal data class SvgRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
)

/** Haut-parleur du téléphone, `h24` dans les deux maquettes. */
private const val SPEAKER_LENGTH = 24f

/** Point ambre du Niumi Point, `r=8` dans les deux maquettes. */
private const val DOT_RADIUS = 8f

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun PhoneNearPointDrawingLightPreview() {
    NiumiPreviewFrame(dark = false) { PhoneNearPointDrawing(contentDescription = "Téléphone et Niumi Point") }
}

@Preview(name = "Sombre, écran 10", widthDp = 390)
@Composable
private fun PhoneNearPointDrawingAlarmPreview() {
    NiumiPreviewFrame(dark = true) {
        PhoneNearPointDrawing(contentDescription = "Téléphone et Niumi Point", variant = PhoneNearPointVariant.Alarm)
    }
}
