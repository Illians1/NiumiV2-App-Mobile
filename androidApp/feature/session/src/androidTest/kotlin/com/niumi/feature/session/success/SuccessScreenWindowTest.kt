package com.niumi.feature.session.success

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.applyNiumiWindowTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Relecture de l'étape 46 : l'écran 11 est toujours clair, même quand la session suivante a déjà
 * rendu la fenêtre sombre (période de 24 h en Quotidien) ; ses barres système passent donc en
 * icônes sombres, et reviennent au thème de la session quand l'écran disparaît.
 */
@RunWith(AndroidJUnit4::class)
class SuccessScreenWindowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var shown by mutableStateOf(true)

    private fun lightStatusBars(): Boolean {
        val activity = composeRule.activity
        return WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars
    }

    @Test
    fun theSuccessScreenLightensTheBarsOfADarkWindowThenGivesThemBack() {
        composeRule.runOnUiThread { composeRule.activity.applyNiumiWindowTheme(dark = true) }
        composeRule.setContent {
            NiumiTheme(dark = true) {
                if (shown) SuccessScreen(state = SuccessUiState.Shown("Debout à 7:04", null, null), onBackHome = {})
            }
        }
        composeRule.waitForIdle()
        assertThat(lightStatusBars()).isTrue()

        shown = false
        composeRule.waitForIdle()
        assertThat(lightStatusBars()).isFalse()
    }
}
