package com.niumi.feature.session.ringtone

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.VolumeRampDurations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Feuille « Sonnerie » (maquettes 18 à 20, SPECIFICATION.md §10, étape 40 révisée le 2026-10-23) :
 * les quatre sons et les quatre segments exposés en radio, l'interrupteur qui masque sous-ligne et
 * barre (D6) ou propose 10 min, la bulle du volume, la croix.
 */
@RunWith(AndroidJUnit4::class)
class RingtoneSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var dismissed = false

    private fun show(initial: RingtoneUiState = RingtoneUiState(isLoading = false, volumePercent = 80)) {
        composeRule.setContent {
            NiumiTheme(dark = false) {
                var state by remember { mutableStateOf(initial) }
                RingtoneSheet(
                    state = state,
                    actions =
                        RingtoneActions(
                            onRingtoneSelected = { key ->
                                state =
                                    state.copy(settings = state.settings.copy(ringtoneKey = key))
                            },
                            onPreviewToggled = {},
                            onVolumeRampEnabledChanged = { enabled ->
                                val seconds = if (enabled) VolumeRampDurations.DEFAULT_SECONDS else null
                                state = state.copy(settings = state.settings.copy(volumeRampSeconds = seconds))
                            },
                            onVolumeRampSelected = { seconds ->
                                state = state.copy(settings = state.settings.copy(volumeRampSeconds = seconds))
                            },
                        ),
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theFourSoundsAreExposedAsRadios() {
        show()

        NiumiRingtones.ALL.forEach { ringtone ->
            composeRule
                .onNodeWithText(ringtone.label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
    }

    @Test
    fun theFourSegmentsAreExposedAsRadiosWithTheChosenDurationSelected() {
        show(RingtoneUiState(isLoading = false, settings = AlarmSoundSettings(volumeRampSeconds = 300)))

        RingtoneTexts.RAMP_LABELS.forEach { label ->
            composeRule
                .onNodeWithText(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
        composeRule
            .onNodeWithText("5 min")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        composeRule
            .onNodeWithContentDescription(RingtoneTexts.RAMP_SWITCH_DESCRIPTION)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    /** Maquette 20 : interrupteur éteint, ni sous-ligne ni barre (D6). */
    @Test
    fun switchingOffHidesTheMentionAndTheBar() {
        show(RingtoneUiState(isLoading = false, settings = AlarmSoundSettings(volumeRampSeconds = 120)))
        composeRule.onNodeWithText(RingtoneTexts.rampMention(120)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(RingtoneTexts.RAMP_SWITCH_DESCRIPTION).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(RingtoneTexts.rampMention(120)).assertDoesNotExist()
        RingtoneTexts.RAMP_LABELS.forEach { label -> composeRule.onNodeWithText(label).assertDoesNotExist() }
        composeRule.onNodeWithText(RingtoneTexts.RAMP_SECTION).assertIsDisplayed()
    }

    /** Allumer propose 10 min (décision du 2026-10-23). */
    @Test
    fun switchingOnProposesTenMinutes() {
        show(RingtoneUiState(isLoading = false, settings = AlarmSoundSettings(volumeRampSeconds = null)))
        composeRule.onNodeWithText("10 min").assertDoesNotExist()

        composeRule.onNodeWithContentDescription(RingtoneTexts.RAMP_SWITCH_DESCRIPTION).performClick()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("10 min")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        composeRule.onNodeWithText(RingtoneTexts.rampMention(600)).assertIsDisplayed()
    }

    /**
     * Le toucher *extérieur* qui ferme la bulle (`PopupProperties.dismissOnClickOutside`) repose
     * sur `ACTION_OUTSIDE`, livré par le `WindowManager` entre deux fenêtres Android distinctes :
     * un mécanisme que l'injection synthétique de `ComposeTestRule` ne traverse pas de façon
     * fiable (vérifié à l'étape 40, avec et sans `NiumiBubble.focusable = true`). Ce test prouve
     * l'ouverture au toucher et la fermeture par un second toucher de la même ligne (même fenêtre,
     * donc fiable en instrumenté) ; le toucher extérieur est vérifié à la main sur appareil
     * (`ETAPE-40.md`) : la bulle se ferme, pas la feuille.
     */
    @Test
    fun touchingTheVolumeRowOpensTheBubbleAndTouchingItAgainClosesIt() {
        show()
        composeRule.onNodeWithText(RingtoneTexts.VOLUME_HINT).assertDoesNotExist()

        composeRule.onNodeWithText(RingtoneTexts.PHONE_VOLUME).performTouchInput { click() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(RingtoneTexts.VOLUME_HINT).assertIsDisplayed()

        composeRule.onNodeWithText(RingtoneTexts.PHONE_VOLUME).performTouchInput { click() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(RingtoneTexts.VOLUME_HINT).assertDoesNotExist()
    }

    @Test
    fun theCrossCallsOnDismiss() {
        show()

        composeRule.onNodeWithContentDescription(RingtoneTexts.CLOSE).performClick()
        composeRule.waitForIdle()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun theSheetPassesTheAccessibilityChecks() {
        show()
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
