package com.niumi.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Ombre des cartes de l'accueil ([NiumiDimens.cardShadow]), couche par couche, dessinée derrière la
 * carte de forme [shape]. [color] est `sectionCardShadow` : transparente en sombre, où les cartes
 * n'ont pas d'ombre, et rien n'est alors dessiné. À placer avant le fond et tout découpage.
 */
internal fun Modifier.niumiCardShadow(
    shape: Shape,
    color: Color,
): Modifier {
    if (color == Color.Transparent) return this
    return NiumiDimens.cardShadow.fold(this) { modifier, layer ->
        modifier.dropShadow(
            shape = shape,
            shadow =
                Shadow(
                    radius = layer.blur,
                    color = color,
                    offset = DpOffset(0.dp, layer.offsetY),
                    alpha = layer.alpha,
                ),
        )
    }
}
