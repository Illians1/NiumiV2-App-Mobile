package com.niumi.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Fondu du bas d'une zone qui défile (décision de Mehdi du 2026-10-22) : il dit qu'il reste du
 * contenu caché, et seulement dans ce cas.
 */
class NiumiScrollFadeTest {
    @Test
    fun nothingHiddenMeansNoFade() {
        assertThat(bottomScrollFadeAlpha(remaining = 0f, fade = 96f)).isEqualTo(0f)
    }

    @Test
    fun moreHiddenThanTheFadeMeansAFullFade() {
        assertThat(bottomScrollFadeAlpha(remaining = 500f, fade = 96f)).isEqualTo(1f)
    }

    @Test
    fun theFadeEasesOutNearTheEnd() {
        assertThat(bottomScrollFadeAlpha(remaining = 48f, fade = 96f)).isEqualTo(0.5f)
    }

    @Test
    fun aFadeWithoutHeightIsNeverDrawn() {
        assertThat(bottomScrollFadeAlpha(remaining = 500f, fade = 0f)).isEqualTo(0f)
    }
}
