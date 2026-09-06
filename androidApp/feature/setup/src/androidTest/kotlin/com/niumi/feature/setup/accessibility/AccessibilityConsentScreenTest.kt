package com.niumi.feature.setup.accessibility

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §12.3 : les cinq points, le bouton d'ouverture des réglages, et aucun clic
 * simulé (le test ne fait que lire l'arbre sémantique, jamais `performClick()`).
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityConsentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screenDisplaysAllFivePointsAndTheSettingsButton() {
        composeRule.setContent {
            AccessibilityConsentScreen(
                state = AccessibilityConsentUiState(isServiceEnabled = false),
                onOpenSettings = {},
            )
        }

        AccessibilityConsentTexts.points.forEach { point ->
            composeRule.onNodeWithText(point, substring = true).assertExists()
        }
        composeRule
            .onNode(hasText(AccessibilityConsentTexts.OPEN_SETTINGS_BUTTON_LABEL).and(hasClickAction()))
            .assertExists()
    }

    @Test
    fun screenReflectsTheEnabledServiceState() {
        composeRule.setContent {
            AccessibilityConsentScreen(
                state = AccessibilityConsentUiState(isServiceEnabled = true),
                onOpenSettings = {},
            )
        }

        composeRule.onNodeWithText("Service actif").assertExists()
    }

    @Test
    fun openSettingsButtonIsTheOnlyClickableNode() {
        composeRule.setContent {
            AccessibilityConsentScreen(
                state = AccessibilityConsentUiState(isServiceEnabled = false),
                onOpenSettings = {},
            )
        }

        val clickableNodes = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertThat(clickableNodes).hasSize(1)
    }
}
