package com.niumi.feature.session.home

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.session.ui.AlarmSoundSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accueil (maquettes 15 à 18, étape 45) : les trois états, le cadenas et la bulle en session, les
 * contrôles automatiques d'accessibilité (décision du 2026-10-20).
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val actions =
        HomeActions(
            onOpenSettings = { calls += "settings" },
            onOpenApps = { calls += "apps" },
            onOpenRingtone = { calls += "ringtone" },
            onCancelWake = { calls += "cancel" },
            onSwitchChanged = { calls += "switch:$it" },
            onDayToggled = { calls += "day:$it" },
            onDayModeSelected = { calls += "mode:$it" },
            onTimeChanged = { handle, minutes -> calls += "time:$handle:$minutes" },
            onDialTapped = { calls += "dial:$it" },
            onPickerConfirmed = { _, _ -> calls += "picker" },
            onPickerDismissed = {},
            onLockedTouch = { calls += "locked" },
            onAnyTouch = {},
        )

    private val scheduled =
        HomeUiState(
            isLoading = false,
            mode = HomeMode.Scheduled,
            bedtimeMinutes = BEDTIME,
            wakeMinutes = WAKE,
            nowMinutes = EVENING,
            enabledIsoDays = setOf(1, 2, 3, 4, 5),
            dayMode = DayMode.CUSTOM,
            centerLabel = HomeTexts.WAKE_IN,
            centerValue = "9 h 26",
            cardTitle = HomeTexts.WAKE_ENABLED,
            cardLines = listOf(HomeTexts.blockingIn(56), HomeTexts.sleep(510)),
            switchChecked = true,
            ringtone = AlarmSoundSummary("Piano", "Progressive · 10 min"),
        )

    private fun show(
        state: HomeUiState,
        dark: Boolean = false,
    ) {
        composeRule.setContent { NiumiTheme(dark = dark) { HomeScreen(state = state, actions = actions) } }
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun wakeScheduledShowsTheCountdownTheCardAndTheSwitchOn() {
        show(scheduled)

        composeRule.onNodeWithText("9 h 26").assertIsDisplayed()
        composeRule.onNodeWithText("Blocage des apps dans 56 min").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Réveil activé")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        composeRule.onNodeWithContentDescription("Vendredi").performClick()

        assertThat(calls).containsExactly("day:5")
    }

    @Test
    fun wakeDisabledKeepsEverythingEditable() {
        show(
            scheduled.copy(
                mode = HomeMode.Disabled,
                centerLabel = HomeTexts.WAKE,
                centerValue = HomeTexts.DISABLED,
                cardTitle = HomeTexts.WAKE_DISABLED,
                cardLines = listOf(HomeTexts.NOTHING_PLANNED),
                switchChecked = false,
            ),
        )

        composeRule.onNodeWithText(HomeTexts.NOTHING_PLANNED).assertIsDisplayed()
        composeRule.onNodeWithText(HomeTexts.DAILY).performClick()
        composeRule.onNodeWithContentDescription("Réveil désactivé").performClick()

        assertThat(calls).containsExactly("mode:DAILY", "switch:true").inOrder()
    }

    @Test
    fun theOneOffTabIsSelectedWhenNoDayIsChecked() {
        show(scheduled.copy(enabledIsoDays = emptySet(), dayMode = DayMode.ONE_OFF))

        composeRule.onNodeWithText(HomeTexts.ONE_OFF).assertIsSelected()
    }

    /** Session en cours : pas d'interrupteur, cadenas, champs verrouillés qui renvoient vers la bulle M10. */
    @Test
    fun inSessionTheFieldsAreLockedAndTheCancelLinkIsShown() {
        show(
            scheduled.copy(
                mode = HomeMode.InSession,
                centerValue = "7 h 50",
                cardTitle = HomeTexts.BLOCKING_IN_PROGRESS,
                cardLines = listOf(HomeTexts.BLOCKED_UNTIL_SCAN),
                message = HomeTexts.LOCKED_UNTIL_SCAN,
            ),
            dark = true,
        )

        composeRule.onNodeWithContentDescription("Réveil activé").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Lundi, Verrouillé").performClick()
        composeRule.onNodeWithText(HomeTexts.CANCEL_WAKE).performClick()
        composeRule.onNodeWithText(HomeTexts.LOCKED_UNTIL_SCAN).assertIsDisplayed()

        assertThat(calls).containsExactly("locked", "cancel").inOrder()
    }

    @Test
    fun theRowsOpenTheirScreens() {
        show(scheduled)

        composeRule.onNodeWithText(HomeTexts.RINGTONE).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(HomeTexts.SETTINGS).performClick()

        assertThat(calls).containsExactly("ringtone", "settings").inOrder()
    }

    private companion object {
        const val BEDTIME = 22 * 60 + 30
        const val WAKE = 7 * 60
        const val EVENING = 21 * 60 + 34
    }
}
