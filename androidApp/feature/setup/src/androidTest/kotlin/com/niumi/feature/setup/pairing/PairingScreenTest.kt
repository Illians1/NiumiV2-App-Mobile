package com.niumi.feature.setup.pairing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.system.nfc.NfcAvailability
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOX_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val TOKEN_FINGERPRINT = "abc123def456abc123def456abc123def456abc123def456abc123def4561234"

/**
 * Écran 3 (SPEC_ANDROID §11.1, §15, §16). Composable pur : l'état est fourni, aucun clic n'est
 * simulé — même convention que `OnboardingScreenTest`.
 */
@RunWith(AndroidJUnit4::class)
class PairingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(state: PairingUiState) {
        composeRule.setContent {
            PairingScreen(
                state = state,
                actions =
                    PairingActions(
                        onOpenNfcSettings = {},
                        onContinue = {},
                    ),
            )
        }
    }

    @Test
    fun theScanInstructionIsShownWhenNfcIsReady() {
        setContent(PairingUiState())

        composeRule.onNodeWithText(PairingTexts.INSTRUCTION, substring = true).assertExists()
    }

    @Test
    fun aDisabledAdapterExplainsHowToFixItInsteadOfAskingForAScan() {
        setContent(PairingUiState(nfcAvailability = NfcAvailability.DISABLED))

        composeRule.onNodeWithText(PairingTexts.NFC_DISABLED, substring = true).assertExists()
        composeRule.onAllNodesWithText(PairingTexts.INSTRUCTION, substring = true).assertCountEquals(0)
        composeRule
            .onNode(hasContentDescription(PairingTexts.OPEN_NFC_SETTINGS_BUTTON_LABEL))
            .assertExists()
    }

    @Test
    fun anAbsentAdapterOffersNoRemedyBecauseThereIsNone() {
        setContent(PairingUiState(nfcAvailability = NfcAvailability.ABSENT))

        composeRule.onNodeWithText(PairingTexts.NFC_ABSENT, substring = true).assertExists()
        composeRule
            .onNode(hasContentDescription(PairingTexts.OPEN_NFC_SETTINGS_BUTTON_LABEL))
            .assertDoesNotExist()
    }

    /** Lot 8 : un doublon se signale par un message, jamais par un dialogue de remplacement. */
    @Test
    fun anAlreadyPairedMessageIsShownWithoutAnyDialog() {
        setContent(PairingUiState(pairedBoxIdPrefix = "11111111", message = PairingTexts.ALREADY_PAIRED))

        composeRule.onNodeWithText(PairingTexts.ALREADY_PAIRED, substring = true).assertExists()
        composeRule.onNode(hasContentDescription(PairingTexts.CONTINUE_BUTTON_LABEL)).assertExists()
    }

    @Test
    fun aPairedBoxAllowsContinuing() {
        setContent(PairingUiState(pairedBoxIdPrefix = "550e8400", message = PairingTexts.PAIRED))

        composeRule.onNode(hasContentDescription(PairingTexts.CONTINUE_BUTTON_LABEL)).assertExists()
    }

    /**
     * SPEC_ANDROID §16 : ni le token ni son empreinte ne doivent apparaître. Seul un préfixe de
     * `boxId` est affiché, jamais l'identifiant entier.
     */
    @Test
    fun neitherTheFingerprintNorTheFullIdentifierIsEverDisplayed() {
        setContent(
            PairingUiState(
                pairedBoxIdPrefix = BOX_ID.take(PairingTexts.BOX_ID_PREFIX_LENGTH),
                message = PairingTexts.PAIRED,
            ),
        )

        composeRule.onAllNodesWithText(TOKEN_FINGERPRINT, substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText(BOX_ID, substring = true).assertCountEquals(0)
    }

    @Test
    fun aSessionInProgressExplainsWhyPairingIsClosed() {
        setContent(PairingUiState(isSessionInProgress = true))

        composeRule.onNodeWithText(PairingTexts.SESSION_IN_PROGRESS, substring = true).performScrollTo().assertExists()
        composeRule.onAllNodesWithText(PairingTexts.INSTRUCTION, substring = true).assertCountEquals(0)
    }

    @Test
    fun theTruncatedIdentifierIsShortEnoughToRevealNothingUseful() {
        assertThat(PairingTexts.BOX_ID_PREFIX_LENGTH).isLessThan(BOX_ID.length)
    }
}
