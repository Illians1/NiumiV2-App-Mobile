package com.niumi.feature.session.blocking

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Écran 7 (étape 39) : les textes de la maquette 22, titre nommant l'application (2026-10-23),
 * deux seuls nœuds cliquables, chacun relié à un seul rappel. Aucun ne touche à la session :
 * l'écran n'en connaît aucune.
 */
@RunWith(AndroidJUnit4::class)
class BlockScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show() {
        composeRule.setContent {
            NiumiTheme(dark = true) {
                BlockScreenContent(
                    title = BlockScreenTexts.title(APP),
                    onClose = { calls += CLOSE },
                    onOpenNiumi = { calls += OPEN },
                )
            }
        }
    }

    @Test
    fun theScreenShowsTheTextsOfTheMockup() {
        show()

        composeRule.onNodeWithText("L’application YouTube est bloquée").assertIsDisplayed()
        composeRule.onNodeWithText(BlockScreenTexts.BODY).assertIsDisplayed()
        composeRule.onNodeWithText(BlockScreenTexts.CLOSE).assertIsDisplayed()
        composeRule.onNodeWithText(BlockScreenTexts.OPEN_NIUMI).assertIsDisplayed()
    }

    @Test
    fun onlyTheTwoButtonsAreClickable() {
        show()

        val labels =
            composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { node ->
                node.config
                    .getOrNull(SemanticsProperties.Text)
                    ?.joinToString { it.text }
                    .orEmpty()
            }
        assertThat(labels).containsExactly(BlockScreenTexts.CLOSE, BlockScreenTexts.OPEN_NIUMI)
    }

    @Test
    fun eachButtonCallsItsOwnActionOnce() {
        show()

        composeRule.onNodeWithText(BlockScreenTexts.CLOSE).performClick()
        composeRule.onNodeWithText(BlockScreenTexts.OPEN_NIUMI).performClick()

        assertThat(calls).containsExactly(CLOSE, OPEN).inOrder()
    }

    @Test
    fun theScreenPassesTheAccessibilityChecks() {
        show()
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private companion object {
        const val CLOSE = "close"
        const val OPEN = "openNiumi"
        const val APP = "YouTube"
    }
}
