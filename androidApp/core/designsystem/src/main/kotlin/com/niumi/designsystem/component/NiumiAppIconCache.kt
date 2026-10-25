package com.niumi.designsystem.component

import android.graphics.drawable.Drawable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Icônes déjà converties, par application ([key], le nom du paquet) et par taille. Sans lui, une case
 * de la grille de l'écran 4 qui revient à l'écran pendant le défilement redessinait son icône sur le
 * fil principal (relecture de l'étape 37). Une icône illisible est retenue aussi : elle n'est pas
 * retentée à chaque retour. Déplacé de `:feature:setup` à l'étape 45 : l'accueil montre les mêmes
 * icônes.
 */
class NiumiAppIconCache(
    private val convert: (Drawable?, Int) -> ImageBitmap? = { icon, sizePx -> icon?.toImageBitmapOrNull(sizePx) },
) {
    private val bitmaps = HashMap<Pair<String, Int>, ImageBitmap?>()

    fun get(
        key: String,
        icon: Drawable?,
        sizePx: Int,
    ): ImageBitmap? {
        val entry = key to sizePx
        if (entry !in bitmaps) bitmaps[entry] = convert(icon, sizePx)
        return bitmaps[entry]
    }
}

/** Fourni par l'écran qui montre des icônes ; par défaut, un cache propre à chaque appel (aperçus, tests). */
val LocalNiumiAppIconCache = staticCompositionLocalOf { NiumiAppIconCache() }
