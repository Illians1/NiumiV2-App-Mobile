package com.niumi.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.unit.max
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

/**
 * Hauteur de la bande du haut d'un écran dont le contenu défile sous elle : la marge du haut de la
 * maquette ou la zone système, la plus grande des deux, comme `marginsOverSystemBars`. À ajouter au
 * début du contenu qui défile, pour qu'il commence sous la bande.
 */
@Composable
fun niumiTopClearance(): Dp =
    max(NiumiDimens.screenTop, WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())

/**
 * Bande posée sur un contenu qui défile dessous et remonte derrière elle (haut de l'accueil, demande
 * de Mehdi du 2026-10-30) : fond [surface] plein, translucide sur ses [fadeHeight] du bas, du plein à
 * [TOP_ALPHA], miroir du haut de la barre d'onglets ; sans ombre. Comme elle, la bande garde tout
 * toucher qu'aucun de ses enfants n'a pris.
 */
internal fun Modifier.niumiHeaderSurface(
    surface: Color,
    fadeHeight: Dp,
): Modifier =
    drawBehind {
        val fade = fadeHeight.toPx().coerceAtMost(size.height)
        drawRect(color = surface, size = Size(size.width, size.height - fade))
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(surface, surface.copy(alpha = TOP_ALPHA)),
                    startY = size.height - fade,
                    endY = size.height,
                ),
            topLeft = Offset(0f, size.height - fade),
            size = Size(size.width, fade),
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
