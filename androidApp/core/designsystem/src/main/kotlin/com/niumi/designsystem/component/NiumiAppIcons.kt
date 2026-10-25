package com.niumi.designsystem.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiOpacity

/**
 * Convertit une icône d'application en [ImageBitmap]. Aucune bibliothèque de chargement d'images
 * n'est au catalogue et aucune n'est ajoutée pour un besoin aussi étroit (CLAUDE.md) : un
 * `Drawable` adaptatif n'a pas de bitmap sous-jacent, il faut le dessiner soi-même.
 *
 * Renvoie `null` plutôt que de lever si l'icône n'a pas de dimension exploitable : une ligne sans
 * icône vaut mieux qu'un sélecteur qui plante.
 */
fun Drawable.toImageBitmapOrNull(sizePx: Int): ImageBitmap? =
    when {
        sizePx <= 0 -> null

        // Une icône déjà matricielle n'a pas besoin d'être redessinée.
        this is BitmapDrawable && bitmap != null -> bitmap.asImageBitmap()

        else -> drawIntoBitmap(sizePx)
    }

private fun Drawable.drawIntoBitmap(sizePx: Int): ImageBitmap? =
    runCatching {
        val target = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        target.asImageBitmap()
    }.getOrNull()

/**
 * Vraie icône d'une application (VALEURS-DE-DESIGN.md §4 : les carrés gris des maquettes sont des
 * emplacements). Sans icône lisible, l'emplacement gris de la maquette. [greyed] grise l'icône :
 * niveaux de gris, à 38 %. Sans description : le nom ou le compte porté par la ligne suffit à TalkBack.
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiAppIcon(
    key: String,
    icon: Drawable?,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    greyed: Boolean = false,
) {
    val palette = LocalNiumiPalette.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val cache = LocalNiumiAppIconCache.current
    val bitmap = remember(key, sizePx) { cache.get(key, icon, sizePx) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
            colorFilter = if (greyed) GREYSCALE else null,
            alpha = if (greyed) NiumiOpacity.GREYED_CONTENT else 1f,
        )
    } else {
        Box(
            modifier =
                modifier
                    .size(size)
                    .alpha(if (greyed) NiumiOpacity.GREYED_CONTENT else 1f)
                    .clip(shape)
                    .background(palette.track)
                    .border(NiumiDimens.hairline, palette.disabled, shape),
        )
    }
}

/** Case « +N » des emplacements d'icônes : non dessinée par les maquettes, sur le modèle des cases vides. */
@Composable
fun NiumiMoreAppsSlot(
    text: String,
    size: Dp,
    shape: Shape,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            modifier
                .size(size)
                .clip(shape)
                .background(palette.track)
                .border(NiumiDimens.hairline, palette.disabled, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = textStyle, color = palette.textSecondary)
    }
}

private val GREYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
