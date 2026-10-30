package com.niumi.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiPalette

/**
 * Ombre des cartes ([NiumiDimens.cardShadow]), couche par couche, dessinée derrière un élément de
 * forme [shape] : le trait de contact puis le halo, aux couleurs de la [palette] (noir, plus opaque en
 * sombre). À placer avant le fond et tout découpage.
 */
internal fun Modifier.niumiCardShadow(
    shape: Shape,
    palette: NiumiPalette,
): Modifier {
    val colors = listOf(palette.cardShadowContact, palette.cardShadowHalo)
    return NiumiDimens.cardShadow.zip(colors).fold(this) { modifier, (layer, color) ->
        modifier.dropShadow(
            shape = shape,
            shadow =
                Shadow(
                    radius = layer.blur,
                    color = color.copy(alpha = 1f),
                    offset = DpOffset(0.dp, layer.offsetY),
                    alpha = color.alpha,
                ),
        )
    }
}
