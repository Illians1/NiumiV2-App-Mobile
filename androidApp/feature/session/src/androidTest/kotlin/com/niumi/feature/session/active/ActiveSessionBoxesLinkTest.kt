package com.niumi.feature.session.active

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lien transitoire « Mes Niumi Spots » de l'écran de session active (décision de Mehdi du 2026-10-24,
 * étape 43) : les Niumi Spots se modifient en session, et l'écran 3 actuel n'était atteignable que par
 * la mise en route. Le lien disparaît avec l'écran à l'étape 45.
 */
@RunWith(AndroidJUnit4::class)
class ActiveSessionBoxesLinkTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theBoxesLinkIsShownDuringASessionAndOpensTheBoxes() {
        var opened = 0
        composeRule.setContent {
            NiumiTheme(dark = false) {
                ActiveSessionScreen(
                    state = ActiveSessionUiState(state = SessionStateDto.ARMED, isLoading = false),
                    actions = ActiveSessionActions(onModifyOrCancel = {}, onOpenBoxes = { opened++ }),
                )
            }
        }

        composeRule
            .onNodeWithText(ActiveSessionTexts.OPEN_BOXES_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertThat(opened).isEqualTo(1)
    }
}
