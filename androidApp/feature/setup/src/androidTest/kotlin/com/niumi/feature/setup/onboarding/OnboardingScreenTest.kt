package com.niumi.feature.setup.onboarding

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §3 (dernier point) : les limites doivent être expliquées **avant** la première
 * activation. Le test prouve qu'elles sont toutes affichées et que rien ne permet de poursuivre
 * sans avoir coché la case. Aucun clic n'est simulé sur la case elle-même : l'état est fourni.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(state: OnboardingUiState) {
        composeRule.setContent {
            OnboardingScreen(state = state, onAcknowledgedChange = {}, onContinue = {})
        }
    }

    @Test
    fun everyLimitIsDisplayed() {
        setContent(OnboardingUiState())

        OnboardingTexts.limits.forEach { limit ->
            composeRule.onNodeWithText(limit, substring = true).performScrollTo().assertExists()
        }
    }

    @Test
    fun theContinueButtonStaysDisabledUntilTheBoxIsChecked() {
        setContent(OnboardingUiState(isAcknowledged = false))

        composeRule
            .onNode(hasText(OnboardingTexts.CONTINUE_BUTTON_LABEL))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun theContinueButtonBecomesEnabledOnceTheBoxIsChecked() {
        setContent(OnboardingUiState(isAcknowledged = true))

        composeRule
            .onNode(hasText(OnboardingTexts.CONTINUE_BUTTON_LABEL))
            .performScrollTo()
            .assertIsEnabled()
    }
}
