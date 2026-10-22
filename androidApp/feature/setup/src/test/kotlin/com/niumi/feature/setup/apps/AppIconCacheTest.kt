package com.niumi.feature.setup.apps

import com.google.common.truth.Truth.assertThat
import com.niumi.system.apps.InstalledApp
import org.junit.Test

/**
 * Une icône n'est convertie qu'une fois par taille : une case qui revient à l'écran pendant le
 * défilement de la grille ne redessine pas l'icône sur le fil principal (relecture de l'étape 37).
 */
class AppIconCacheTest {
    private val conversions = mutableListOf<Pair<String, Int>>()
    private val cache = AppIconCache { app, sizePx -> null.also { conversions += app.packageName to sizePx } }
    private val instagram = InstalledApp("com.instagram.android", "Instagram", icon = null)

    @Test
    fun anIconIsConvertedOncePerSize() {
        cache.get(instagram, sizePx = 154)
        cache.get(instagram, sizePx = 154)
        cache.get(instagram, sizePx = 110)

        assertThat(
            conversions,
        ).containsExactly("com.instagram.android" to 154, "com.instagram.android" to 110).inOrder()
    }

    /** Une icône illisible n'est pas retentée à chaque retour de la case. */
    @Test
    fun aMissingIconIsRememberedToo() {
        assertThat(cache.get(instagram, sizePx = 154)).isNull()
        assertThat(cache.get(instagram, sizePx = 154)).isNull()

        assertThat(conversions).hasSize(1)
    }
}
