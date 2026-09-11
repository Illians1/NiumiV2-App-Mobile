package com.niumi.feature.setup.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

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
