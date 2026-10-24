package com.niumi.feature.setup.pairing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
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

private fun row(
    index: Int,
    name: String,
): PairedBoxRow {
    val boxId = "${index.toString().repeat(8)}-1111-1111-1111-111111111111"
    return PairedBoxRow(boxId, PairingTexts.prefixOf(boxId), name)
}

/**
 * Écran 3 « Mes Niumi Spots » (SPEC_ANDROID §11.1, §15 Lot 8, §16). Composable pur : l'état est
 * fourni, aucun clic n'est simulé.
 */
@RunWith(AndroidJUnit4::class)
class PairingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(state: PairingUiState) {
        composeRule.setContent {
            PairingScreen(
                state = state,
                actions = PairingActions(onOpenNfcSettings = {}, onContinue = {}),
            )
        }
    }

    @Test
    fun theScanInstructionIsShownWhileScanning() {
        setContent(PairingUiState(isScanning = true))

        composeRule.onNodeWithText(PairingTexts.INSTRUCTION, substring = true).assertExists()
        composeRule.onNode(hasContentDescription(PairingTexts.CANCEL_LABEL)).assertExists()
        composeRule.onNode(hasContentDescription(PairingTexts.ADD_BUTTON_LABEL)).assertDoesNotExist()
    }

    /** Décision du 2026-10-17 : avec des Niumi Spots, la lecture attend « Ajouter un Niumi Spot ». */
    @Test
    fun outsideAScanTheAddButtonReplacesTheInstruction() {
        setContent(PairingUiState(boxes = listOf(row(1, "Cuisine"))))

        composeRule.onNode(hasContentDescription(PairingTexts.ADD_BUTTON_LABEL)).assertExists()
        composeRule.onAllNodesWithText(PairingTexts.INSTRUCTION, substring = true).assertCountEquals(0)
    }

    @Test
    fun theListShowsEachBoxByNameAndPrefixWithItsTwoActions() {
        setContent(PairingUiState(boxes = listOf(row(1, "Salle de bain"), row(2, "Cuisine"))))

        composeRule.onNodeWithText("Salle de bain").assertExists()
        composeRule.onNodeWithText("11111111…").assertExists()
        composeRule.onNodeWithText("Cuisine").assertExists()
        composeRule.onNodeWithText("22222222…").assertExists()
        composeRule.onNode(hasContentDescription(PairingTexts.renameDescription("Cuisine"))).assertIsEnabled()
        composeRule.onNode(hasContentDescription(PairingTexts.removeDescription("Cuisine"))).assertIsEnabled()
    }

    /** TalkBack : la liste annonce sa taille, faute de rôle « élément de liste » dans Compose. */
    @Test
    fun theListAnnouncesItsSizeToAccessibilityServices() {
        setContent(PairingUiState(boxes = listOf(row(1, "Salle de bain"), row(2, "Cuisine"))))

        composeRule
            .onNode(
                SemanticsMatcher("liste de 2") { node ->
                    node.config.getOrElseNullable(SemanticsProperties.CollectionInfo) { null }?.rowCount == 2
                },
            ).assertExists()
    }

    @Test
    fun tenBoxesHideTheAddButtonAndSayWhy() {
        // Dix depuis l'étape 42 (`PairedBoxLimit.MAX`) : le test en citait encore cinq.
        setContent(PairingUiState(boxes = (1..10).map { row(it, "Niumi Spot $it") }))

        composeRule.onNodeWithText(PairingTexts.LIMIT_REACHED).performScrollTo().assertExists()
        composeRule.onNode(hasContentDescription(PairingTexts.ADD_BUTTON_LABEL)).assertDoesNotExist()
    }

    @Test
    fun aDisabledAdapterExplainsHowToFixItInsteadOfAskingForAScan() {
        setContent(PairingUiState(nfcAvailability = NfcAvailability.DISABLED, isScanning = true))

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
        setContent(PairingUiState(boxes = listOf(row(1, "Cuisine")), message = PairingTexts.ALREADY_PAIRED))

        composeRule.onNodeWithText(PairingTexts.ALREADY_PAIRED, substring = true).assertExists()
        composeRule.onAllNodesWithText(PairingTexts.ADD_DIALOG_TITLE).assertCountEquals(0)
        composeRule.onNode(hasContentDescription(PairingTexts.CONTINUE_BUTTON_LABEL)).assertExists()
    }

    @Test
    fun aPendingNameOpensTheDialogPrefilledAndCounted() {
        setContent(PairingUiState(pendingName = "Niumi Spot 3"))

        composeRule.onNodeWithText(PairingTexts.ADD_DIALOG_TITLE).assertExists()
        composeRule.onNodeWithText("Niumi Spot 3").assertExists()
        composeRule.onNodeWithText("12/30").assertExists()
        composeRule.onNodeWithText(PairingTexts.CONFIRM_ADD_LABEL).assertIsEnabled()
    }

    @Test
    fun aBlankNameCannotBeConfirmed() {
        setContent(PairingUiState(pendingName = "   "))

        composeRule.onNodeWithText(PairingTexts.CONFIRM_ADD_LABEL).assertIsNotEnabled()
    }

    @Test
    fun aRenamingOpensTheRenameDialog() {
        setContent(
            PairingUiState(
                boxes = listOf(row(1, "Cuisine")),
                renaming = Renaming(row(1, "Cuisine").boxId, "Cuisine"),
            ),
        )

        val rowAction = hasContentDescription(PairingTexts.renameDescription("Cuisine"))
        composeRule.onNodeWithText(PairingTexts.RENAME_DIALOG_TITLE).assertExists()
        // « Renommer » est aussi l'action de la ligne, inactive tant que le dialogue attend sa réponse.
        composeRule
            .onAllNodesWithText(PairingTexts.CONFIRM_RENAME_LABEL)
            .filterToOne(!rowAction)
            .assertIsEnabled()
        composeRule.onNode(rowAction).assertIsNotEnabled()
    }

    @Test
    fun aBoxAllowsContinuing() {
        setContent(PairingUiState(boxes = listOf(row(1, "Cuisine")), message = PairingTexts.BOX_ADDED))

        composeRule.onNode(hasContentDescription(PairingTexts.CONTINUE_BUTTON_LABEL)).assertExists()
    }

    @Test
    fun withoutAnyBoxThereIsNothingToContinueWith() {
        setContent(PairingUiState(isScanning = true))

        composeRule.onNode(hasContentDescription(PairingTexts.CONTINUE_BUTTON_LABEL)).assertDoesNotExist()
    }

    /**
     * SPEC_ANDROID §16 : ni le token ni son empreinte ne doivent apparaître. Seul un préfixe de
     * `boxId` est affiché, jamais l'identifiant entier.
     */
    @Test
    fun neitherTheFingerprintNorTheFullIdentifierIsEverDisplayed() {
        setContent(
            PairingUiState(
                boxes = listOf(PairedBoxRow(BOX_ID, PairingTexts.prefixOf(BOX_ID), "Cuisine")),
                message = PairingTexts.BOX_ADDED,
            ),
        )

        composeRule.onAllNodesWithText(TOKEN_FINGERPRINT, substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText(BOX_ID, substring = true).assertCountEquals(0)
        composeRule.onNodeWithText("550e8400…").assertExists()
    }

    /**
     * Étape 43 (SPEC_CORE_KMP §2, décisions 11 et 16) : en session, ajout, renommage et retrait
     * restent actifs ; seul « Continuer » disparaît (décision de Mehdi du 2026-10-24).
     */
    @Test
    fun aSessionInProgressKeepsEveryBoxActionButHidesContinue() {
        setContent(
            PairingUiState(
                boxes = listOf(row(1, "Salle de bain"), row(2, "Cuisine")),
                isSessionInProgress = true,
            ),
        )

        composeRule.onNode(hasContentDescription(PairingTexts.renameDescription("Cuisine"))).assertIsEnabled()
        composeRule.onNode(hasContentDescription(PairingTexts.removeDescription("Cuisine"))).assertIsEnabled()
        composeRule.onNode(hasContentDescription(PairingTexts.ADD_BUTTON_LABEL)).performScrollTo().assertIsEnabled()
        composeRule.onNode(hasContentDescription(PairingTexts.CONTINUE_BUTTON_LABEL)).assertDoesNotExist()
    }

    /** Décision Q22 (étape 43) : le dernier Niumi Spot se renomme mais ne se retire pas, sans texte ajouté. */
    @Test
    fun aSingleBoxCanBeRenamedButNotRemoved() {
        setContent(PairingUiState(boxes = listOf(row(1, "Cuisine"))))

        composeRule.onNode(hasContentDescription(PairingTexts.renameDescription("Cuisine"))).assertIsEnabled()
        composeRule.onNode(hasContentDescription(PairingTexts.removeDescription("Cuisine"))).assertDoesNotExist()
        composeRule.onAllNodesWithText(PairingTexts.REMOVE_LABEL).assertCountEquals(0)
    }

    @Test
    fun theTruncatedIdentifierIsShortEnoughToRevealNothingUseful() {
        assertThat(PairingTexts.BOX_ID_PREFIX_LENGTH).isLessThan(BOX_ID.length)
    }
}
