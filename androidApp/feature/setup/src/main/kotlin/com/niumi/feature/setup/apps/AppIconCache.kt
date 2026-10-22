package com.niumi.feature.setup.apps

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.niumi.system.apps.InstalledApp

/**
 * Icônes déjà converties, par application et par taille. Sans lui, une case de la grille qui
 * revient à l'écran pendant le défilement redessinait son icône sur le fil principal (relecture de
 * l'étape 37). Une icône illisible est retenue aussi : elle n'est pas retentée à chaque retour.
 * Vit le temps de l'écran 4 ; la carte et la page le partagent.
 */
class AppIconCache(
    private val convert: (InstalledApp, Int) -> ImageBitmap? = { app, sizePx -> app.icon?.toImageBitmapOrNull(sizePx) },
) {
    private val bitmaps = HashMap<Pair<String, Int>, ImageBitmap?>()

    fun get(
        app: InstalledApp,
        sizePx: Int,
    ): ImageBitmap? {
        val key = app.packageName to sizePx
        if (key !in bitmaps) bitmaps[key] = convert(app, sizePx)
        return bitmaps[key]
    }
}

/** Fourni par l'écran 4 ; par défaut, un cache propre à chaque appel (aperçus, tests). */
internal val LocalAppIconCache = staticCompositionLocalOf { AppIconCache() }
