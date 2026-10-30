package com.niumi.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Feuille de scan (étape 38, maquettes 29 à 33) : « Prêt à scanner » et ses textes, « Annuler » et
 * le voile qui ferment une fois, bascule vers un échec sans refermer la feuille, bouton d'échec qui
 * agit sans fermer, lien de secours absent quand l'écran n'en fournit pas.
 */
@RunWith(AndroidJUnit4::class)
class NfcScanSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var dismissals = 0
    private var failureActions = 0
    private var content: NfcScanSheetContent by mutableStateOf(NfcScanSheetContent.Ready)

    private fun showSheet(phrase: String = NiumiComponentTexts.SCAN_SHEET_PHRASE) {
        composeRule.setContent {
            NiumiTheme(dark = true) {
                NfcScanSheet(
                    content = content,
                    onDismiss = { dismissals++ },
                    onFailureAction = { failureActions++ },
                    failurePaneTitle = FAILURE_PANE,
                    phrase = phrase,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theReadySheetShowsTheTextsOfTheMockup() {
        showSheet()

        composeRule
            .onNodeWithText(NiumiComponentTexts.SCAN_SHEET_TITLE)
            .assertIsDisplayed()
            .assert(isHeading())
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_PHRASE).assertIsDisplayed()
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_CANCEL).assertIsDisplayed()
    }

    /** Étape 46 : la feuille d'annulation dit pourquoi on scanne (`SPECIFICATION.md` §11). */
    @Test
    fun theScreenCanGiveItsOwnPhrase() {
        showSheet(phrase = CANCEL_PHRASE)

        composeRule.onNodeWithText(CANCEL_PHRASE).assertIsDisplayed()
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_PHRASE).assertDoesNotExist()
    }

    /**
     * Étape 46 (choix de Mehdi du 2026-10-26) : pendant la libération, la feuille ne montre que le
     * cercle de chargement, lu par TalkBack, après le délai habituel.
     */
    @Test
    fun theReleasingContentShowsOnlyTheLoader() {
        content = NfcScanSheetContent.Releasing(LOADER_LABEL)
        showSheet()
        composeRule.mainClock.advanceTimeBy(NiumiLoadingIndicatorDefaults.DELAY_MILLIS + 100)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(LOADER_LABEL).assertExists()
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_CANCEL).assertDoesNotExist()
    }

    /** Deux touchers pendant l'animation de fermeture, horloge arrêtée : une seule fermeture. */
    @Test
    fun cancelClosesTheSheetOnce() {
        showSheet()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_CANCEL).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_CANCEL).performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertThat(dismissals).isEqualTo(1)
    }

    @Test
    fun touchingTheVeilClosesTheSheet() {
        showSheet()
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

    /** L'échec remplace « Prêt à scanner » dans la même feuille : rien ne se ferme. */
    @Test
    fun aFailureReplacesTheReadyContentWithoutClosing() {
        showSheet()

        content = FAILURE
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText(FAILURE.title)
            .assertIsDisplayed()
            .assert(isHeading())
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        composeRule.onNodeWithText(FAILURE.help).assertIsDisplayed()
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_TITLE).assertDoesNotExist()
        assertThat(dismissals).isEqualTo(0)
    }

    @Test
    fun theFailureButtonActsWithoutClosing() {
        content = FAILURE
        showSheet()

        composeRule.onNodeWithText(FAILURE.actionLabel).performClick()
        composeRule.waitForIdle()

        assertThat(failureActions).isEqualTo(1)
        assertThat(dismissals).isEqualTo(0)
    }

    @Test
    fun withoutAnEmergencyLinkTheFailureHasOnlyItsButton() {
        content = FAILURE
        showSheet()

        composeRule.onNodeWithText(EMERGENCY_LINK).assertDoesNotExist()
    }

    @Test
    fun theEmergencyLinkIsShownWhenTheScreenProvidesIt() {
        var exits = 0
        composeRule.setContent {
            NiumiTheme(dark = true) {
                NfcScanSheet(
                    content = FAILURE,
                    onDismiss = {},
                    onFailureAction = {},
                    failurePaneTitle = FAILURE_PANE,
                    emergencyLinkText = EMERGENCY_LINK,
                    onEmergencyExit = { exits++ },
                )
            }
        }

        composeRule.onNodeWithText(EMERGENCY_LINK).performClick()

        assertThat(exits).isEqualTo(1)
    }

    /** La feuille est une fenêtre à part : deux racines, toutes deux contrôlées. */
    @Test
    fun bothContentsPassTheAccessibilityChecks() {
        showSheet()
        composeRule.enableAccessibilityChecks()
        checkEveryWindow()

        content = FAILURE
        composeRule.waitForIdle()
        checkEveryWindow()
    }

    private fun checkEveryWindow() {
        val roots = composeRule.onAllNodes(isRoot())
        repeat(roots.fetchSemanticsNodes().size) { roots[it].tryPerformAccessibilityChecks() }
    }

    private companion object {
        const val FAILURE_PANE = "Échec du scan"
        const val LOADER_LABEL = "Annulation du réveil"
        const val CANCEL_PHRASE = "Approche ton téléphone du Niumi Spot pour annuler le réveil"
        const val EMERGENCY_LINK = "Je n’ai pas accès à mon Niumi Spot"
        val FAILURE =
            NfcScanSheetContent.Failure(
                title = "Ce n’est pas un Niumi Spot",
                help = "Scanne un Niumi Spot que tu as enregistré.",
                actionLabel = "Réessayer",
            )
    }
}
