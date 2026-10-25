package com.niumi.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Une icône n'est convertie qu'une fois par taille : une case qui revient à l'écran pendant le
 * défilement de la grille ne redessine pas l'icône sur le fil principal (relecture de l'étape 37).
 * Déplacé de `:feature:setup` avec le cache à l'étape 45.
 */
class NiumiAppIconCacheTest {
    private val conversions = mutableListOf<Pair<String, Int>>()
    private var current = ""
    private val cache = NiumiAppIconCache { _, sizePx -> null.also { conversions += current to sizePx } }
    private val instagram = "com.instagram.android"

    private fun NiumiAppIconCache.get(
        key: String,
        sizePx: Int,
    ): androidx.compose.ui.graphics.ImageBitmap? {
        current = key
        return get(key, icon = null, sizePx = sizePx)
    }

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
