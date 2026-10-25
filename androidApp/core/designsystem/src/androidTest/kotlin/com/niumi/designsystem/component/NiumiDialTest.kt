package com.niumi.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cadran de l'accueil (étape 45, décisions de Mehdi du 2026-10-25) : chaque poignée est un réglage
 * annoncé par son nom et son heure ; un double toucher ouvre le sélecteur sur la bonne heure ; le
 * réglage TalkBack avance de 5 minutes ; un toucher hors des poignées ouvre l'heure du réveil.
 */
@RunWith(AndroidJUnit4::class)
class NiumiDialTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val changes = mutableListOf<Pair<DialHandle, Int>>()
    private val taps = mutableListOf<DialHandle>()

    private fun show(state: DialState) {
        composeRule.setContent {
            NiumiTheme(dark = state is DialState.InSession) {
                NiumiDial(
                    state = state,
                    texts = DialTexts(bedtime = "Coucher", wake = "Réveil", chooseTime = "Choisir l’heure"),
                    onTimeChanged = { handle, minutes -> changes += handle to minutes },
                    onTap = { taps += it },
                )
            }
        }
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private val scheduled = DialState.Scheduled(BEDTIME, WAKE, NOW, "Réveil dans", "9 h 26")

    @Test
    fun eachHandleIsAnnouncedWithItsTime() {
        show(scheduled)

        // Le centre se lit aussi, hors session comme en session (relecture sur appareil).
        composeRule.onNodeWithText("9 h 26").assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Coucher")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "22:30"))
        composeRule
            .onNodeWithContentDescription("Réveil")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "7:00"))
    }

    @Test
    fun activatingAHandleOpensThePickerOnItsTime() {
        show(scheduled)

        composeRule.onNodeWithContentDescription("Coucher").performClick()

        assertThat(taps).containsExactly(DialHandle.BEDTIME)
    }

    @Test
    fun theAccessibilityAdjustmentMovesByFiveMinutes() {
        show(scheduled)

        composeRule.onNodeWithContentDescription("Réveil").performSemanticsAction(SemanticsActions.SetProgress) {
            it(WAKE + 5f)
        }

        assertThat(changes).containsExactly(DialHandle.WAKE to WAKE + 5)
    }

    @Test
    fun aTapAwayFromTheHandlesOpensTheWakeTime() {
        show(scheduled)

        composeRule.onRoot().performTouchInput { click(center) }

        assertThat(taps).containsExactly(DialHandle.WAKE)
        assertThat(changes).isEmpty()
    }

    /** Relecture : un glissé hors des poignées (un défilement, une poignée manquée) n'ouvre rien. */
    @Test
    fun aSwipeAwayFromTheHandlesOpensNothing() {
        show(scheduled)

        composeRule.onRoot().performTouchInput { swipe(center, center + Offset(0f, 200f)) }

        assertThat(taps).isEmpty()
        assertThat(changes).isEmpty()
    }

    @Test
    fun inSessionTheDialHasNoHandlesAndReadsItsCenter() {
        show(DialState.InSession(BEDTIME, WAKE, BEDTIME + 40, "Réveil dans", "7 h 50"))

        composeRule.onNodeWithContentDescription("Coucher", substring = true).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Coucher 22:30, Réveil 7:00"),
            ),
        )
        // Pas de réglage « Coucher » seul : les poignées n'existent pas en session.
        composeRule.onNodeWithContentDescription("Coucher").assertDoesNotExist()
    }

    private companion object {
        const val BEDTIME = 22 * 60 + 30
        const val WAKE = 7 * 60
        const val NOW = 21 * 60 + 34
    }
}
