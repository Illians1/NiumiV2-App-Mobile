package com.niumi.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Indicateur de chargement (décision de Mehdi du 2026-10-22, sans maquette) : rien pendant les
 * 300 premières millisecondes, pour ne pas clignoter quand la réponse est rapide, puis un cercle
 * animé que TalkBack annonce par son libellé.
 */
@RunWith(AndroidJUnit4::class)
class NiumiLoadingIndicatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theIndicatorAppearsOnlyAfterTheDelay() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { NiumiTheme(dark = false) { NiumiLoadingIndicator(label = "Chargement") } }

        composeRule.mainClock.advanceTimeBy(NiumiLoadingIndicatorDefaults.DELAY_MILLIS - FRAME)
        composeRule.onNodeWithContentDescription("Chargement").assertDoesNotExist()

        composeRule.mainClock.advanceTimeBy(FRAME * 2)
        composeRule.onNodeWithContentDescription("Chargement").assertExists()
    }

    private companion object {
        const val FRAME = 32L
    }
}
