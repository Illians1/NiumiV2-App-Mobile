package com.niumi.feature.setup.readiness

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §13 : « L'écran n'affiche qu'une action principale à la fois, en commençant par le
 * premier blocage. » §15 exige en plus TalkBack, d'où la `contentDescription` sur l'action.
 */
@RunWith(AndroidJUnit4::class)
class ReadinessScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun item(
        id: ReadinessCheckId,
        outcome: ReadinessOutcome,
        action: ReadinessAction,
        available: Boolean,
    ) = ReadinessItem(
        id = id,
        message = ReadinessMessages.forCheck(id),
        label = ReadinessMessages.labelFor(id),
        severity = ReadinessSeverityDto.BLOCKING_FOR_ALARM,
        outcome = outcome,
        action = action,
        actionLabel = ReadinessMessages.actionLabelFor(id),
        isActionAvailable = available,
    )

    private fun stateWith(
        primary: ReadinessItem,
        others: List<ReadinessItem>,
    ) = ReadinessUiState(
        items = listOf(primary) + others,
        primary = primary,
        isAllowed = false,
        isLoading = false,
    )

    @Test
    fun onlyThePrimaryFailureCarriesAClickableAction() {
        val primary =
            item(ReadinessCheckId.ALARM_VOLUME, ReadinessOutcome.FAILED, ReadinessAction.OpenSoundSettings, true)
        val others =
            listOf(
                item(
                    ReadinessCheckId.DND_TOTAL_SILENCE,
                    ReadinessOutcome.FAILED,
                    ReadinessAction.OpenDndSettings,
                    true,
                ),
                item(
                    ReadinessCheckId.ACCESSIBILITY_SERVICE,
                    ReadinessOutcome.FAILED,
                    ReadinessAction.OpenAccessibilitySettings,
                    true,
                ),
            )
        composeRule.setContent {
            ReadinessScreen(state = stateWith(primary, others), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        val clickableNodes = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertThat(clickableNodes).hasSize(1)
    }

    @Test
    fun thePrimaryActionCarriesItsLabelAsAContentDescription() {
        val primary =
            item(ReadinessCheckId.ALARM_VOLUME, ReadinessOutcome.FAILED, ReadinessAction.OpenSoundSettings, true)
        composeRule.setContent {
            ReadinessScreen(state = stateWith(primary, emptyList()), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        composeRule
            .onNode(hasContentDescription(ReadinessMessages.actionLabelFor(ReadinessCheckId.ALARM_VOLUME)))
            .assertExists()
    }

    @Test
    fun anActionWithoutADestinationIsShownDisabledRatherThanHidden() {
        val primary =
            item(ReadinessCheckId.PAIRED_BOX, ReadinessOutcome.FAILED, ReadinessAction.StartPairing, false)
        composeRule.setContent {
            ReadinessScreen(state = stateWith(primary, emptyList()), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        composeRule.onNodeWithText(ReadinessMessages.forCheck(ReadinessCheckId.PAIRED_BOX)).assertExists()
        composeRule
            .onNodeWithText(ReadinessMessages.actionLabelFor(ReadinessCheckId.PAIRED_BOX))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun theExactAlarmFailureExplainsItselfInsteadOfOfferingASetting() {
        val primary =
            item(
                ReadinessCheckId.EXACT_ALARM,
                ReadinessOutcome.FAILED,
                ReadinessAction.ShowExactAlarmDiagnostic,
                true,
            )
        composeRule.setContent {
            ReadinessScreen(state = stateWith(primary, emptyList()), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        composeRule.onNodeWithText(ReadinessMessages.EXACT_ALARM_DIAGNOSTIC).performScrollTo().assertExists()
    }

    @Test
    fun aSatisfiedCheckIsNamedAndNeverDescribedByItsFailure() {
        // Régression du 2026-09-11, trouvée sur appareil : la liste affichait
        // « ✓ Le volume des alarmes est à zéro » pour un volume correct (§15).
        val primary =
            item(ReadinessCheckId.PAIRED_BOX, ReadinessOutcome.FAILED, ReadinessAction.StartPairing, false)
        val passed =
            item(ReadinessCheckId.ALARM_VOLUME, ReadinessOutcome.PASSED, ReadinessAction.OpenSoundSettings, true)
        composeRule.setContent {
            ReadinessScreen(state = stateWith(primary, listOf(passed)), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        composeRule
            .onNodeWithText(ReadinessMessages.labelFor(ReadinessCheckId.ALARM_VOLUME), substring = true)
            .performScrollTo()
            .assertExists()
        composeRule
            .onAllNodesWithText(ReadinessMessages.forCheck(ReadinessCheckId.ALARM_VOLUME), substring = true)
            .assertCountEquals(0)
    }

    /**
     * Défaut mesuré sur appareil à l'étape 13 : une fois le boîtier associé, sa ligne passait au
     * vert et perdait son bouton, rendant l'écran 3 inatteignable alors que §11.1 autorise une
     * nouvelle association. Une étape de parcours satisfaite garde donc un recours.
     */
    @Test
    fun aSatisfiedJourneyCheckKeepsAWayBackToItsScreen() {
        val primary =
            item(ReadinessCheckId.ALARM_VOLUME, ReadinessOutcome.FAILED, ReadinessAction.OpenSoundSettings, true)
        val pairedBox =
            item(ReadinessCheckId.PAIRED_BOX, ReadinessOutcome.PASSED, ReadinessAction.StartPairing, true)
                .copy(actionLabel = ReadinessMessages.CHANGE_PAIRED_BOX_LABEL)
        composeRule.setContent {
            ReadinessScreen(state = stateWith(primary, listOf(pairedBox)), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        composeRule
            .onNode(hasContentDescription(ReadinessMessages.CHANGE_PAIRED_BOX_LABEL))
            .performScrollTo()
            .assertExists()
        // Le libellé de première association contredirait la ligne verte juste au-dessus (§15).
        composeRule
            .onAllNodesWithText(ReadinessMessages.actionLabelFor(ReadinessCheckId.PAIRED_BOX))
            .assertCountEquals(0)
    }

    @Test
    fun aSatisfiedBlockingCheckOffersNoWayBack() {
        val primary =
            item(ReadinessCheckId.PAIRED_BOX, ReadinessOutcome.FAILED, ReadinessAction.StartPairing, true)
        val passedVolume =
            item(ReadinessCheckId.ALARM_VOLUME, ReadinessOutcome.PASSED, ReadinessAction.OpenSoundSettings, true)
        composeRule.setContent {
            ReadinessScreen(
                state = stateWith(primary, listOf(passedVolume)),
                onPrimaryAction = {},
                onChooseWakeTime = {},
            )
        }

        val clickableNodes = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertThat(clickableNodes).hasSize(1)
    }

    @Test
    fun aReadyDeviceSaysSoOnce() {
        composeRule.setContent {
            ReadinessScreen(state = ReadinessUiState(isLoading = false), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        composeRule.onNodeWithText(ReadinessMessages.ALL_CLEAR).assertExists()
    }

    /**
     * Sortie vers l'écran 5 (étape 14). Sans elle, le choix de l'heure serait inatteignable :
     * `ReadinessAction.FixTime` n'apparaît jamais tant qu'aucune heure candidate n'existe.
     */
    @Test
    fun aReadyDeviceOffersTheWayToTheWakeTimeScreen() {
        composeRule.setContent {
            ReadinessScreen(state = ReadinessUiState(isLoading = false), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        composeRule
            .onNode(hasContentDescription(ReadinessMessages.CHOOSE_WAKE_TIME_LABEL))
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun aDeviceWithABlockingCheckOffersNoWayToTheWakeTimeScreen() {
        val primary =
            item(ReadinessCheckId.ALARM_VOLUME, ReadinessOutcome.FAILED, ReadinessAction.OpenSoundSettings, true)
        composeRule.setContent {
            ReadinessScreen(state = stateWith(primary, emptyList()), onPrimaryAction = {}, onChooseWakeTime = {})
        }

        composeRule
            .onAllNodesWithText(ReadinessMessages.CHOOSE_WAKE_TIME_LABEL)
            .assertCountEquals(0)
    }
}
