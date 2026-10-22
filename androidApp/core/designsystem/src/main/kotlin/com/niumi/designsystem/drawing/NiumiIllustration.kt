package com.niumi.designsystem.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Socle des dessins des maquettes : une zone de [NiumiDimens.illustrationWidth] ×
 * [NiumiDimens.illustrationHeight] au plus, réduite en gardant ses proportions sur un écran étroit,
 * annoncée une seule fois comme image par [contentDescription] (du `Texts` de l'écran).
 *
 * [draw] dessine dans le repère du `viewBox` de la maquette (342 × 240) : [IllustrationScope]
 * convertit chaque coordonnée et chaque épaisseur à la taille réelle. Les nombres des dessins sont
 * donc des coordonnées de la maquette, pas des dimensions de l'écran.
 *
 * [maxWidth] et [maxHeight] : taille affichée au plus, 342 × 240 par défaut ; l'écran 10 montre
 * son dessin en 264 × 170 (maquette 28, `viewBox` 342 × 220). La largeur du `viewBox` reste 342.
 */
@Composable
internal fun NiumiIllustration(
    contentDescription: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = NiumiDimens.illustrationWidth,
    maxHeight: Dp = NiumiDimens.illustrationHeight,
    draw: IllustrationScope.() -> Unit,
) {
    Canvas(
        modifier =
            modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .aspectRatio(maxWidth / maxHeight)
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    role = Role.Image
                },
    ) {
        IllustrationScope(this, size.width / VIEW_BOX_WIDTH).draw()
    }
}

/** Primitives des dessins, en coordonnées du `viewBox` de la maquette. */
internal class IllustrationScope(
    val drawScope: DrawScope,
    private val scale: Float,
) {
    fun at(
        x: Float,
        y: Float,
    ) = Offset(x * scale, y * scale)

    /** Trait arrondi aux extrémités et aux angles (`stroke-linecap` et `stroke-linejoin: round`). */
    fun stroke(width: Float) = Stroke(width = width * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

    @Suppress("LongParameterList") // Les attributs d'un `<rect>` SVG, un par paramètre.
    fun roundRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Color,
        strokeWidth: Float? = null,
    ) = drawScope.drawRoundRect(
        color = color,
        topLeft = at(x, y),
        size = Size(width * scale, height * scale),
        cornerRadius = CornerRadius(radius * scale),
        style = strokeWidth?.let(::stroke) ?: Fill,
    )

    fun circle(
        cx: Float,
        cy: Float,
        radius: Float,
        color: Color,
    ) = drawScope.drawCircle(color = color, radius = radius * scale, center = at(cx, cy))

    fun line(
        from: Offset,
        to: Offset,
        color: Color,
        strokeWidth: Float,
    ) = drawScope.drawLine(
        color = color,
        start = from,
        end = to,
        strokeWidth = strokeWidth * scale,
        cap = StrokeCap.Round,
    )
}

/** Largeur du `viewBox` des maquettes 01 et 02. */
internal const val VIEW_BOX_WIDTH = 342f

/** Épaisseur du trait des dessins des maquettes 01 et 02. */
internal const val ILLUSTRATION_STROKE = 3f
