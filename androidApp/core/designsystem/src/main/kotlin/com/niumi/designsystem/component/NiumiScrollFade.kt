package com.niumi.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Fondu du bas d'une zone qui défile (décision de Mehdi du 2026-10-22, VALEURS-DE-DESIGN.md §2) :
 * tant que du contenu est caché sous le bas, il s'estompe dans le fond du thème sur
 * [NiumiDimens.scrollFadeHeight]. Il n'apparaît pas quand tout tient, et s'efface à mesure qu'on
 * approche de la fin. À poser **avant** `verticalScroll` : il se dessine sur la fenêtre visible,
 * pas sur le contenu.
 */
@Composable
fun Modifier.niumiBottomScrollFade(state: ScrollState): Modifier {
    val background = LocalNiumiPalette.current.background
    return drawWithContent {
        drawContent()
        val fade = NiumiDimens.scrollFadeHeight.toPx()
        // Lu ici, le défilement ne redessine que cette couche, sans recomposer l'écran.
        val alpha = bottomScrollFadeAlpha(remaining = (state.maxValue - state.value).toFloat(), fade = fade)
        if (alpha > 0f) {
            drawRect(
                brush = Brush.verticalGradient(listOf(background.copy(alpha = 0f), background)),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
                alpha = alpha,
            )
        }
    }
}

/**
 * Opacité du fondu selon la distance qui reste à faire défiler : pleine tant qu'il reste au moins
 * la hauteur du fondu, nulle au bout ou quand rien ne défile.
 */
internal fun bottomScrollFadeAlpha(
    remaining: Float,
    fade: Float,
): Float = if (fade <= 0f) 0f else (remaining / fade).coerceIn(0f, 1f)
