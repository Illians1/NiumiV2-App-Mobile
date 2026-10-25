package com.niumi.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Hauteur que la barre d'onglets occupe sur le contenu : 66 et la barre système. À ajouter à la fin
 * du contenu qui défile sous la barre, pour que sa dernière ligne puisse remonter au-dessus d'elle.
 */
@Composable
fun niumiTabBarClearance(): Dp =
    NiumiDimens.tabBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Surface de la barre d'onglets, posée sur le contenu qui défile dessous (demande de Mehdi du
 * 2026-10-25) :
 * - fond [surface] translucide en haut, de [TOP_ALPHA] au plein sur
 *   [NiumiDimens.tabBarTranslucentHeight], pour laisser deviner ce qui passe derrière ; plein
 *   ensuite, barre système comprise. Seul le fond est translucide, pas le contenu de la barre ;
 * - l'ombre de la maquette 16 (4 vers le haut, flou 16, noir à 8 %), en dégradé de
 *   [NiumiDimens.tabBarShadowHeight] au-dessus : une élévation se verrait à travers le fond ;
 * - tout toucher qu'aucun onglet n'a pris reste à la barre : rien ne traverse jusqu'au contenu.
 */
internal fun Modifier.niumiTabBarSurface(surface: Color): Modifier =
    drawBehind {
        val shadow = NiumiDimens.tabBarShadowHeight.toPx()
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = SHADOW_ALPHA)),
                    startY = -shadow,
                    endY = 0f,
                ),
            topLeft = Offset(0f, -shadow),
            size = Size(size.width, shadow),
        )
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(surface.copy(alpha = TOP_ALPHA), surface),
                    startY = 0f,
                    endY = NiumiDimens.tabBarTranslucentHeight.toPx(),
                ),
        )
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Final).changes.forEach { it.consume() }
            }
        }
    }

private const val TOP_ALPHA = 0.72f
private const val SHADOW_ALPHA = 0.08f
