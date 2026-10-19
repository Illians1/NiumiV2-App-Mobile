package com.niumi.designsystem.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `NiumiTheme(dark)` fournit la palette demandée et le `MaterialTheme` qui en dérive, sans jamais
 * consulter le réglage clair ou sombre du téléphone (étape 33).
 */
@RunWith(AndroidJUnit4::class)
class NiumiThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun darkProvidesTheDarkPaletteAndItsMaterialScheme() {
        assertProvided(dark = true, expected = NiumiPalettes.Dark)
    }

    @Test
    fun lightProvidesTheLightPaletteAndItsMaterialScheme() {
        assertProvided(dark = false, expected = NiumiPalettes.Light)
    }

    private fun assertProvided(
        dark: Boolean,
        expected: NiumiPalette,
    ) {
        var palette: NiumiPalette? = null
        var background: Color? = null
        var typography: Typography? = null
        composeRule.setContent {
            NiumiTheme(dark = dark) {
                palette = LocalNiumiPalette.current
                background = MaterialTheme.colorScheme.background
                typography = MaterialTheme.typography
            }
        }
        composeRule.waitForIdle()

        assertThat(palette).isEqualTo(expected)
        assertThat(background).isEqualTo(expected.background)
        assertThat(typography).isEqualTo(NiumiType.material())
    }
}
