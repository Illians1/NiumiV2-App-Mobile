package com.niumi.feature.setup.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * Écran 1 (`SPECIFICATION.md` §4, maquettes 01 et 02) : chaque panneau montre ses textes, son
 * dessin et son repère ; la flèche de retour n'existe que sur le panneau 2 ; chaque bouton appelle
 * son action et elle seule. L'état est fourni, comme pour les autres écrans de la mise en route.
 */
@RunWith(AndroidJUnit4::class)
class PresentationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun setContent(panel: Int) {
        composeRule.setContent {
            NiumiTheme(dark = false) {
                PresentationScreen(
                    state = PresentationUiState(panel = panel),
                    actions =
                        PresentationActions(
                            onContinue = { calls += "continue" },
                            onBack = { calls += "back" },
                            onConfigure = { calls += "configure" },
                        ),
                )
            }
        }
    }

    @Test
    fun theFirstPanelShowsItsTextsDrawingAndIndicator() {
        setContent(panel = 1)

        composeRule.onNodeWithText(PresentationTexts.PANEL_1_TITLE).assertExists()
        composeRule.onNodeWithText(PresentationTexts.PANEL_1_BODY).assertExists()
        composeRule.onNodeWithContentDescription(PresentationTexts.PANEL_1_DRAWING).assertExists()
        composeRule.onNodeWithContentDescription(PresentationTexts.PANEL_1_INDICATOR).assertExists()
        composeRule.onNodeWithContentDescription("niumi").assertExists()
    }

    @Test
    fun theFirstPanelHasNoBackArrow() {
        setContent(panel = 1)

        composeRule.onAllNodesWithContentDescription(PresentationTexts.BACK).assertCountEquals(0)
    }

    @Test
    fun continueLeadsToTheSecondPanel() {
        setContent(panel = 1)

        composeRule.onNodeWithText(PresentationTexts.CONTINUE).performClick()

        assertThat(calls).containsExactly("continue")
    }

    @Test
    fun theSecondPanelShowsItsTextsDrawingAndIndicator() {
        setContent(panel = 2)

        composeRule.onNodeWithText(PresentationTexts.PANEL_2_TITLE).assertExists()
        composeRule.onNodeWithText(PresentationTexts.PANEL_2_BODY).assertExists()
        composeRule.onNodeWithContentDescription(PresentationTexts.PANEL_2_DRAWING).assertExists()
        composeRule.onNodeWithContentDescription(PresentationTexts.PANEL_2_INDICATOR).assertExists()
        composeRule.onAllNodesWithText(PresentationTexts.CONTINUE).assertCountEquals(0)
    }

    @Test
    fun theSecondPanelArrowGoesBack() {
        setContent(panel = 2)

        composeRule.onNodeWithContentDescription(PresentationTexts.BACK).performClick()

        assertThat(calls).containsExactly("back")
    }

    @Test
    fun configureIsTheOnlyWayForward() {
        setContent(panel = 2)

        composeRule.onNodeWithText(PresentationTexts.CONFIGURE).performClick()

        assertThat(calls).containsExactly("configure")
    }

    /**
     * Contrôles automatiques de l'Accessibility Test Framework (libellés, zones de toucher,
     * contraste, ordre de lecture), décision du 2026-10-20 : activés sur chaque écran refait.
     */
    @Test
    fun theFirstPanelPassesTheAccessibilityChecks() {
        setContent(panel = 1)
        composeRule.enableAccessibilityChecks()

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theSecondPanelPassesTheAccessibilityChecks() {
        setContent(panel = 2)
        composeRule.enableAccessibilityChecks()

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
