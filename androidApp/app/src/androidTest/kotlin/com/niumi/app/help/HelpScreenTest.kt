package com.niumi.app.help

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Écran 13 (SPEC_ANDROID §15, étape 21). Le composable est pur : il reçoit tout de [HelpTexts] et
 * n'a ni ViewModel ni Hilt, donc le test le rend directement.
 */
@RunWith(AndroidJUnit4::class)
class HelpScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Une page longue : sans `performScrollTo`, les dernières limites ne seraient jamais
     * composées et le test passerait en n'ayant rien vérifié (même piège qu'`OnboardingScreenTest`).
     */
    @Test
    fun everyLimitIsDisplayed() {
        composeRule.setContent { NiumiTheme { HelpScreen() } }

        HelpTexts.sections.forEach { section ->
            composeRule.onNodeWithText(section.title, substring = true).performScrollTo().assertExists()
            section.items.forEach { item ->
                composeRule.onNodeWithText(item, substring = true).performScrollTo().assertExists()
            }
        }
    }

    /**
     * §3 et §10.2 : le scan du boîtier est la seule sortie de session. Un écran de consultation
     * atteignable pendant une session ne doit offrir **aucune** action — c'est ce qu'un bouton
     * ajouté par mégarde ferait perdre, sans qu'aucun autre test ne s'en aperçoive.
     */
    @Test
    fun theHelpScreenOffersNoAction() {
        composeRule.setContent { NiumiTheme { HelpScreen() } }

        val clickableNodes = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()

        assertThat(clickableNodes).isEmpty()
    }
}
