package com.niumi.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Feuille du bas (VALEURS-DE-DESIGN.md §4, maquettes 16, 19, 23) : titre en intertitre, croix qui
 * ferme, et toucher du voile qui ferme comme sur iPhone.
 */
@RunWith(AndroidJUnit4::class)
class NiumiBottomSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var dismissals = 0

    private fun showSheet(closeLabel: String?) {
        composeRule.setContent {
            NiumiTheme(dark = true) {
                NiumiBottomSheet(
                    onDismiss = { dismissals++ },
                    title = "Annuler la session ?",
                    closeLabel = closeLabel,
                ) { requestClose ->
                    Text("Scanne ton Niumi Point pour annuler.")
                    NiumiPrimaryButton(text = "Rester dans la session", onClick = requestClose)
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theTitleIsShownAsAHeading() {
        showSheet(closeLabel = null)

        composeRule
            .onNodeWithText("Annuler la session ?")
            .assertIsDisplayed()
            .assert(isHeading())
        composeRule.onNodeWithText("Scanne ton Niumi Point pour annuler.").assertIsDisplayed()
    }

    @Test
    fun theCrossClosesTheSheet() {
        showSheet(closeLabel = "Fermer")

        composeRule
            .onNodeWithContentDescription("Fermer")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        composeRule.waitForIdle()

        assertThat(dismissals).isEqualTo(1)
    }

    /**
     * Croix, croix encore, puis bouton du contenu, pendant l'animation de fermeture : [onDismiss]
     * n'est appelé qu'une fois (relecture de l'étape 34 ; vu à 2 avant correction).
     */
    @Test
    fun quickRepeatedClosingRequestsCloseTheSheetOnce() {
        showSheet(closeLabel = "Fermer")
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription("Fermer").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithContentDescription("Fermer").performClick()
        composeRule.onNodeWithText("Rester dans la session").performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertThat(dismissals).isEqualTo(1)
    }

    /** Un bouton du contenu (« Rester dans la session ») range la feuille comme la croix. */
    @Test
    fun theContentCanCloseTheSheet() {
        showSheet(closeLabel = null)

        composeRule.onNodeWithText("Rester dans la session").performClick()
        composeRule.waitForIdle()

        assertThat(dismissals).isEqualTo(1)
        // Rangée hors de l'écran ; c'est à l'écran appelant de la retirer de la composition.
        composeRule.onNodeWithText("Annuler la session ?").assertIsNotDisplayed()
    }

    @Test
    fun withoutACloseLabelThereIsNoCross() {
        showSheet(closeLabel = null)

        composeRule.onNodeWithContentDescription("Fermer").assertDoesNotExist()
    }

    /** Le voile couvre tout l'écran ; on le touche en haut à gauche, loin de la feuille. */
    @Test
    fun touchingTheVeilClosesTheSheet() {
        showSheet(closeLabel = null)
        val veilLabel =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(androidx.compose.ui.R.string.close_sheet)

        composeRule
            .onNode(hasContentDescription(veilLabel) and hasClickAction())
            .performTouchInput { click(Offset(x = 10f, y = 10f)) }
        composeRule.waitForIdle()

        assertThat(dismissals).isEqualTo(1)
    }
}
