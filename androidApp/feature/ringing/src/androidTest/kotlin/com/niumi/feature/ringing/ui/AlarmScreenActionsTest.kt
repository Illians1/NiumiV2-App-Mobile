package com.niumi.feature.ringing.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.designsystem.component.NiumiComponentTexts
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanSheetState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Garde-fou de SPEC_ANDROID §19.1 depuis l'étape 38 (remplace `AlarmScreenNoStopActionTest`, qui
 * interdisait tout nœud cliquable avant que l'écran 10 ait un bouton) : **liste blanche** des nœuds
 * cliquables par état, chacun relié à une seule action de [AlarmScreenActions]. Aucune n'arrête
 * l'alarme ni ne touche la session : elles ouvrent, referment ou rouvrent la feuille, ou ouvrent les
 * réglages NFC (`AlarmViewModelTest` prouve qu'elles n'écrivent rien).
 */
@RunWith(AndroidJUnit4::class)
class AlarmScreenActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private var screen: AlarmScreenState? by mutableStateOf(null)

    private val actions =
        AlarmScreenActions(
            openScanSheet = { calls += OPEN },
            cancelScan = { calls += CANCEL },
            retryScan = { calls += RETRY },
            openNfcSettings = { calls += SETTINGS },
        )

    private val veilLabel: String =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(androidx.compose.ui.R.string.close_sheet)

    /** Le contenu n'est posé qu'une fois ; les appels suivants changent seulement l'état affiché. */
    private fun show(state: AlarmScreenState) {
        val first = screen == null
        screen = state
        if (first) {
            composeRule.setContent {
                NiumiTheme(dark = true) {
                    screen?.let { AlarmScreen(state = it, now = NOW, actions = actions) }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** Libellé de chaque nœud cliquable, toutes fenêtres comprises (la feuille est une fenêtre). */
    private fun clickableLabels(): List<String> =
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { node ->
            node.config
                .getOrNull(SemanticsProperties.Text)
                ?.joinToString { it.text }
                ?: node.config
                    .getOrNull(SemanticsProperties.ContentDescription)
                    ?.joinToString()
                    .orEmpty()
        }

    @Test
    fun theRingingScreenOffersOnlyTheScanButton() {
        show(AlarmScreenState.from(SessionStateDto.RINGING))

        assertThat(clickableLabels()).containsExactly(AlarmTexts.SCAN_BUTTON)
        composeRule.onNodeWithText(AlarmTexts.SCAN_BUTTON).performClick()
        assertThat(calls).containsExactly(OPEN)
    }

    @Test
    fun theThreeWaitingStatesOfferTheSameSingleButton() {
        listOf(
            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
        ).forEach { session ->
            show(AlarmScreenState.from(session))
            assertThat(clickableLabels()).containsExactly(AlarmTexts.SCAN_BUTTON)
            composeRule.onNodeWithText(AlarmTexts.INSTRUCTION).assertExists()
        }
    }

    @Test
    fun theReadySheetAddsOnlyCancelAndTheVeil() {
        show(AlarmScreenState.from(SessionStateDto.RINGING, sheet = ScanSheetState.Ready))

        assertThat(clickableLabels())
            .containsExactly(AlarmTexts.SCAN_BUTTON, NiumiComponentTexts.SCAN_SHEET_CANCEL, veilLabel)
        composeRule.onNodeWithText(NiumiComponentTexts.SCAN_SHEET_CANCEL).performClick()
        composeRule.waitForIdle()
        assertThat(calls).containsExactly(CANCEL)
    }

    @Test
    fun touchingTheVeilOnlyClosesTheSheet() {
        show(AlarmScreenState.from(SessionStateDto.RINGING, sheet = ScanSheetState.Ready))

        composeRule
            .onNode(hasContentDescription(veilLabel) and hasClickAction())
            .performTouchInput { click(Offset(x = 10f, y = 10f)) }
        composeRule.waitForIdle()

        assertThat(calls).containsExactly(CANCEL)
    }

    @Test
    fun aScanFailureAddsOnlyRetryAndTheVeil() {
        listOf(
            ScanFailureReason.NOT_A_NIUMI_POINT,
            ScanFailureReason.NOT_REGISTERED,
            ScanFailureReason.UNREADABLE,
        ).forEach { reason ->
            calls.clear()
            show(AlarmScreenState.from(SessionStateDto.RINGING, sheet = ScanSheetState.Failed(reason)))

            val failure = AlarmTexts.failure(reason)
            assertThat(clickableLabels()).containsExactly(AlarmTexts.SCAN_BUTTON, failure.actionLabel, veilLabel)
            composeRule.onNodeWithText(failure.title).assertExists()
            composeRule.onNodeWithText(failure.help).assertExists()
            composeRule.onNodeWithText(failure.actionLabel).performClick()
            assertThat(calls).containsExactly(RETRY)
        }
    }

    @Test
    fun nfcTurnedOffAddsOnlyTheSettingsButtonAndTheVeil() {
        show(
            AlarmScreenState.from(
                SessionStateDto.RINGING,
                nfcAvailability = NfcAvailability.DISABLED,
                sheet = ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED),
            ),
        )

        val failure = AlarmTexts.failure(ScanFailureReason.NFC_DISABLED)
        assertThat(clickableLabels()).containsExactly(AlarmTexts.SCAN_BUTTON, failure.actionLabel, veilLabel)
        composeRule.onNodeWithText(failure.actionLabel).performClick()
        assertThat(calls).containsExactly(SETTINGS)
    }

    /** Le scan a eu lieu : plus rien à toucher ; la date, l'heure et le chargement seuls. */
    @Test
    fun releasingOffersNothingToTouch() {
        show(AlarmScreenState.from(SessionStateDto.RELEASING))

        assertThat(clickableLabels()).isEmpty()
        composeRule.onNodeWithText(AlarmTexts.INSTRUCTION).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(AlarmTexts.DRAWING).assertDoesNotExist()
        composeRule.waitUntil(LOADER_TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription(AlarmTexts.RELEASE_LOADER).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun noLabelSpeaksOfStoppingOrSnoozing() {
        val labels =
            listOf(
                AlarmScreenState.from(SessionStateDto.RINGING),
                AlarmScreenState.from(SessionStateDto.RINGING, sheet = ScanSheetState.Ready),
                AlarmScreenState.from(
                    SessionStateDto.RINGING,
                    sheet = ScanSheetState.Failed(ScanFailureReason.NOT_REGISTERED),
                ),
            ).flatMap {
                show(it)
                clickableLabels()
            }

        assertThat(labels.filter { label -> STOP_WORDS.any { label.contains(it, ignoreCase = true) } }).isEmpty()
    }

    @Test
    fun theScreenPassesTheAccessibilityChecks() {
        show(AlarmScreenState.from(SessionStateDto.RINGING))
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private companion object {
        const val OPEN = "openScanSheet"
        const val CANCEL = "cancelScan"
        const val RETRY = "retryScan"
        const val SETTINGS = "openNfcSettings"
        const val LOADER_TIMEOUT_MS = 2_000L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 2, 7, 0)
        val STOP_WORDS = listOf("arrêt", "stop", "snooze", "report", "plus tard", "couper", "éteindre")
    }
}
