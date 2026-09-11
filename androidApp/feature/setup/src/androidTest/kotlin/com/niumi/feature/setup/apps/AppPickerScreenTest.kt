package com.niumi.feature.setup.apps

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.system.apps.InstalledApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Écran 4 (SPEC_ANDROID §12.1, §15). Composable pur : l'état est fourni, aucun clic n'est simulé.
 */
@RunWith(AndroidJUnit4::class)
class AppPickerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun item(
        packageName: String,
        label: String,
        isSelected: Boolean = false,
        showPackageName: Boolean = false,
    ) = AppPickerItem(InstalledApp(packageName, label, icon = null), isSelected, showPackageName)

    private fun setContent(state: AppPickerUiState) {
        composeRule.setContent {
            AppPickerScreen(state = state, onToggle = {}, onConfirm = {})
        }
    }

    @Test
    fun theCounterShowsTheSelectionAgainstTheCommonUpperBound() {
        setContent(
            AppPickerUiState(
                items = listOf(item("com.example.a", "Agenda", isSelected = true), item("com.example.b", "Banque")),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText(AppPickerTexts.counterLabel(1)).assertExists()
        composeRule.onNodeWithText("/ ${AppSelectionSummary.MAX_COUNT}", substring = true).assertExists()
    }

    @Test
    fun confirmationIsDisabledWithNothingSelected() {
        setContent(AppPickerUiState(items = listOf(item("com.example.a", "Agenda")), isLoading = false))

        composeRule.onNode(hasContentDescription(AppPickerTexts.CONFIRM_BUTTON_LABEL)).assertIsNotEnabled()
    }

    @Test
    fun confirmationIsEnabledWithOneSelectedApplication() {
        setContent(
            AppPickerUiState(items = listOf(item("com.example.a", "Agenda", isSelected = true)), isLoading = false),
        )

        composeRule.onNode(hasContentDescription(AppPickerTexts.CONFIRM_BUTTON_LABEL)).assertIsEnabled()
    }

    /** §12.1 : le nom de package n'apparaît que si plusieurs applications partagent un libellé. */
    @Test
    fun thePackageNameIsShownOnlyOnRowsWhoseLabelIsDuplicated() {
        setContent(
            AppPickerUiState(
                items =
                    listOf(
                        item("com.vendor.a.chat", "Chat", showPackageName = true),
                        item("com.vendor.b.chat", "Chat", showPackageName = true),
                        item("com.example.unique", "Unique"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("com.vendor.a.chat").assertExists()
        composeRule.onNodeWithText("com.vendor.b.chat").assertExists()
        composeRule.onAllNodesWithText("com.example.unique").assertCountEquals(0)
    }

    @Test
    fun theRefusalMessageIsShownWhenTheUpperBoundIsReached() {
        setContent(
            AppPickerUiState(
                items = listOf(item("com.example.a", "Agenda", isSelected = true)),
                isLoading = false,
                message = AppPickerTexts.TOO_MANY_MESSAGE,
            ),
        )

        composeRule.onNodeWithText(AppPickerTexts.TOO_MANY_MESSAGE, substring = true).assertExists()
    }

    @Test
    fun aSessionInProgressBlocksConfirmationAndSaysWhy() {
        setContent(
            AppPickerUiState(
                items = listOf(item("com.example.a", "Agenda", isSelected = true)),
                isLoading = false,
                message = AppPickerTexts.SESSION_IN_PROGRESS,
                isSessionInProgress = true,
            ),
        )

        composeRule.onNodeWithText(AppPickerTexts.SESSION_IN_PROGRESS, substring = true).assertExists()
        composeRule.onNode(hasContentDescription(AppPickerTexts.CONFIRM_BUTTON_LABEL)).assertIsNotEnabled()
    }

    @Test
    fun anEmptyDeviceSaysSoInsteadOfShowingAnEmptyList() {
        setContent(AppPickerUiState(items = emptyList(), isLoading = false))

        composeRule.onNodeWithText(AppPickerTexts.EMPTY, substring = true).assertExists()
    }

    @Test
    fun everyRowCarriesItsLabelForTalkBack() {
        setContent(
            AppPickerUiState(items = listOf(item("com.example.a", "Agenda")), isLoading = false),
        )

        composeRule.onNode(hasContentDescription("Agenda")).assertExists()
    }
}
