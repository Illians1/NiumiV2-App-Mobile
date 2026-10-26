package com.niumi.feature.session.success

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Écran 11 · Réussite (étape 46, maquette 42) : textes, « Bravo ! » seulement quand il y a de quoi,
 * « Retour à l'accueil ». Toujours clair (§15) : l'écran pose lui-même son thème.
 */
@RunWith(AndroidJUnit4::class)
class SuccessScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var backHome = 0

    private fun show(state: SuccessUiState.Shown) {
        composeRule.setContent { SuccessScreen(state = state, onBackHome = { backHome++ }) }
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theExampleOfTheMockupShowsEveryLine() {
        show(
            SuccessUiState.Shown(
                "Debout à 7:04.",
                "4 minutes après la sonnerie.",
                "C’est 11 minutes de moins qu’avant Niumi.",
            ),
        )

        composeRule.onNodeWithText("Debout à 7:04.").assertIsDisplayed()
        composeRule.onNodeWithText("4 minutes après la sonnerie.").assertIsDisplayed()
        composeRule.onNodeWithText("Bravo ! C’est 11 minutes de moins qu’avant Niumi.").assertIsDisplayed()
        composeRule.onNodeWithText(SuccessTexts.APPS_BACK).assertIsDisplayed()
    }

    @Test
    fun withoutDelayNorComparisonNothingTakesTheirPlace() {
        show(SuccessUiState.Shown("Debout à 9:00.", null, null))

        composeRule.onNodeWithText("Debout à 9:00.").assertIsDisplayed()
        composeRule.onNodeWithText("Bravo", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("après la sonnerie", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(SuccessTexts.APPS_BACK).assertIsDisplayed()
    }

    @Test
    fun theButtonGoesBackHome() {
        show(SuccessUiState.Shown("Debout à 7:04.", "4 minutes après la sonnerie.", null))

        composeRule.onNodeWithText(SuccessTexts.BACK_HOME).performClick()

        assertThat(backHome).isEqualTo(1)
    }
}
