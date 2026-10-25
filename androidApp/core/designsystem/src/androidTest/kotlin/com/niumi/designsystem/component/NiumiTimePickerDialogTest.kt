package com.niumi.designsystem.component

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sélecteur d'heure (maquettes 19 et 20, étape 45) : réglage à la minute, onglets qui gardent le choix
 * en cours, « Valider » qui rend les deux heures, la croix qui ne change rien.
 */
@RunWith(AndroidJUnit4::class)
class NiumiTimePickerDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed: Pair<Int, Int>? = null
    private var dismissed = false

    private fun show(initial: DialHandle) {
        composeRule.setContent {
            NiumiTheme(dark = false) {
                NiumiTimePickerDialog(
                    initialHandle = initial,
                    bedtimeMinutes = 22 * 60 + 30,
                    wakeMinutes = 7 * 60,
                    texts =
                        TimePickerTexts(
                            "Coucher",
                            "Réveil",
                            "Heure de coucher",
                            "Heure de réveil",
                            "Heures",
                            "Minutes",
                            "Valider",
                            "Fermer",
                        ),
                    sentence = { handle, bedtime, wake -> "$handle $bedtime $wake" },
                    onConfirm = { bedtime, wake -> confirmed = bedtime to wake },
                    onDismiss = { dismissed = true },
                )
            }
        }
    }

    @Test
    fun aMinuteChosenOnTheBedtimeTabIsConfirmedWithTheUnchangedWakeTime() {
        show(DialHandle.BEDTIME)

        composeRule
            .onNodeWithContentDescription(
                "Minutes",
            ).performSemanticsAction(SemanticsActions.SetProgress) { it(31f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Valider").performClick()

        assertThat(confirmed).isEqualTo(22 * 60 + 31 to 7 * 60)
    }

    @Test
    fun switchingTabsKeepsTheChoiceInProgress() {
        show(DialHandle.WAKE)

        composeRule
            .onNodeWithContentDescription(
                "Heures",
            ).performSemanticsAction(SemanticsActions.SetProgress) { it(6f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Coucher").performClick()
        composeRule.onNodeWithText("BEDTIME", substring = true).assertExists()
        composeRule.onNodeWithText("Valider").performClick()

        assertThat(confirmed).isEqualTo(22 * 60 + 30 to 6 * 60)
    }

    @Test
    fun theCrossClosesWithoutConfirming() {
        show(DialHandle.WAKE)

        composeRule.onNodeWithContentDescription("Fermer").performClick()

        assertThat(dismissed).isTrue()
        assertThat(confirmed).isNull()
    }
}
