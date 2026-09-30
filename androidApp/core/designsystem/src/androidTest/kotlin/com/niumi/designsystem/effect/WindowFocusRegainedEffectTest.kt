package com.niumi.designsystem.effect

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Écart 10 de `RELEASE_REPORT.md` : fermer le volet rapide rend le focus à la fenêtre sans
 * `ON_RESUME`. L'effet ne doit réagir qu'à ce retour, jamais à la composition ni à la perte.
 */
@RunWith(AndroidJUnit4::class)
class WindowFocusRegainedEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class FakeWindowInfo(
        focused: Boolean,
    ) : WindowInfo {
        override var isWindowFocused by mutableStateOf(focused)
    }

    private val window = FakeWindowInfo(focused = true)
    private var calls = 0
    private var composed by mutableStateOf(true)

    private fun setContent() {
        composeRule.setContent {
            CompositionLocalProvider(LocalWindowInfo provides window) {
                if (composed) WindowFocusRegainedEffect { calls++ }
            }
        }
        composeRule.waitForIdle()
    }

    private fun focus(value: Boolean) {
        composeRule.runOnIdle { window.isWindowFocused = value }
        composeRule.waitForIdle()
    }

    @Test
    fun theInitialCompositionDoesNotCallBack() {
        setContent()

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun losingTheFocusDoesNotCallBack() {
        setContent()

        focus(false)

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun eachRegainedFocusCallsBackOnce() {
        setContent()

        focus(false)
        focus(true)
        focus(false)
        focus(true)

        assertThat(calls).isEqualTo(2)
    }

    @Test
    fun aWindowUnfocusedAtCompositionCallsBackWhenItGetsTheFocus() {
        window.isWindowFocused = false
        setContent()

        focus(true)

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun nothingIsCalledOnceTheEffectLeavesTheComposition() {
        setContent()
        composeRule.runOnIdle { composed = false }
        composeRule.waitForIdle()

        focus(false)
        focus(true)

        assertThat(calls).isEqualTo(0)
    }
}
