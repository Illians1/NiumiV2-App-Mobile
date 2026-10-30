package com.niumi.feature.session.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.component.NiumiComponentTexts
import com.niumi.designsystem.component.NiumiLoadingIndicatorDefaults
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanFailureTexts
import com.niumi.system.nfc.ScanSheetState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fenêtre « Annuler le réveil ? » (étape 46, maquettes 25 et 26 ; fenêtre centrée depuis le
 * 2026-10-30) : la question, sa croix et son bouton, la feuille de scan avec la phrase de
 * l'annulation, les échecs de l'écran 10, aucun lien de secours avant l'étape 49. Thème sombre : la
 * fenêtre ne s'ouvre qu'en session.
 */
@RunWith(AndroidJUnit4::class)
class CancelSessionSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private var state by mutableStateOf(CancelSessionUiState())

    private val actions =
        CancelSessionActions(
            onDismiss = { calls += "dismiss" },
            onScan = {
                calls += "scan"
                state = state.copy(sheet = ScanSheetState.Ready)
            },
            onCancelScan = {
                calls += "cancelScan"
                state = state.copy(sheet = ScanSheetState.Hidden)
            },
            onRetry = { calls += "retry" },
            onOpenNfcSettings = { calls += "nfcSettings" },
        )

    private fun show() {
        composeRule.setContent { NiumiTheme(dark = true) { CancelSessionSheet(state = state, actions = actions) } }
        composeRule.waitForIdle()
    }

    @Test
    fun theQuestionShowsTheTextsOfTheSpecificationAndNoEmergencyLinkYet() {
        show()

        composeRule.onNodeWithText(CancelSessionTexts.TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(CancelSessionTexts.BODY).assertIsDisplayed()
        composeRule.onNodeWithText(CancelSessionTexts.SCAN_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("Je n’ai pas accès à mon Niumi Spot").assertDoesNotExist()
    }

    /** Fenêtre centrée au format de l'alerte (demande de Mehdi du 2026-10-30) : la croix ferme sans rien changer. */
    @Test
    fun theCloseCrossDismissesTheQuestion() {
        show()

        composeRule.onNodeWithContentDescription(HomeTexts.CLOSE).performClick()

        assertThat(calls).containsExactly("dismiss")
    }

    @Test
    fun theScanButtonOpensTheScanSheetWithTheCancellationPhrase() {
        show()

        composeRule.onNodeWithText(CancelSessionTexts.SCAN_BUTTON).performClick()
        composeRule.waitForIdle()

        assertThat(calls).containsExactly("scan")
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(CancelSessionTexts.SCAN_PHRASE).assertIsDisplayed()
        composeRule.onNodeWithText(CancelSessionTexts.TITLE).assertDoesNotExist()
    }

    @Test
    fun cancellingTheScanSheetComesBackToTheQuestion() {
        state = CancelSessionUiState(sheet = ScanSheetState.Ready)
        show()

        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_CANCEL).performClick()
        composeRule.waitForIdle()

        assertThat(calls).containsExactly("cancelScan")
        composeRule.onNodeWithText(CancelSessionTexts.TITLE).assertIsDisplayed()
    }

    @Test
    fun aFailureShowsTheTextsOfTheAlarmScreenAndRetries() {
        state = CancelSessionUiState(sheet = ScanSheetState.Failed(ScanFailureReason.NOT_REGISTERED))
        show()
        val copy = ScanFailureTexts.of(ScanFailureReason.NOT_REGISTERED)

        composeRule.onNodeWithText(copy.title).assertIsDisplayed()
        composeRule.onNodeWithText(copy.actionLabel).performClick()

        assertThat(calls).containsExactly("retry")
    }

    @Test
    fun aDisabledNfcOpensItsSettings() {
        state = CancelSessionUiState(sheet = ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED))
        show()

        composeRule.onNodeWithText(ScanFailureTexts.of(ScanFailureReason.NFC_DISABLED).actionLabel).performClick()

        assertThat(calls).containsExactly("nfcSettings")
    }

    /** Choix de Mehdi du 2026-10-26 : pendant la libération, le cercle seul, lu « Annulation du réveil ». */
    @Test
    fun whileReleasingTheScanSheetShowsTheLoader() {
        state = CancelSessionUiState(sheet = ScanSheetState.Ready, releasing = true)
        show()
        composeRule.mainClock.advanceTimeBy(NiumiLoadingIndicatorDefaults.DELAY_MILLIS + 100)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(CancelSessionTexts.RELEASE_LOADER).assertExists()
        composeRule.onNodeWithText(CancelSessionTexts.SCAN_PHRASE).assertDoesNotExist()
    }

    @Test
    fun theSheetPassesTheAccessibilityChecks() {
        show()
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
