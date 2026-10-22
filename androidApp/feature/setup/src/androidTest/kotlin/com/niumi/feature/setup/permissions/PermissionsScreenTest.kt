package com.niumi.feature.setup.permissions

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.setup.accessibility.AccessibilityConsentTexts
import com.niumi.system.permissions.NiumiPermission
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessMessages
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Écran 2 (`SPECIFICATION.md` §5, maquette 03, tuiles Android du 2026-10-21) : textes, titre
 * compté, coche « Accordée » à la place d'« Autoriser », « Continuer » inactif tant qu'une tuile
 * manque, chaque bouton n'appelant que son action. Les fenêtres centrées (consentement, verrou,
 * alarme exacte) portent une croix et se ferment sans rien faire d'autre que relire. Pas de « ? »
 * tant que le guide est reporté (M1).
 */
@RunWith(AndroidJUnit4::class)
class PermissionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    /**
     * [granted] : tuiles accordées ; [shown] : tuiles affichées (sans `RECENTS_LOCK`, un téléphone
     * sans verrou des récents). Par défaut : Alarme, Batterie et Verrouillage accordés.
     */
    private fun setContent(
        granted: Set<NiumiPermission> =
            setOf(NiumiPermission.ALARM, NiumiPermission.BATTERY, NiumiPermission.RECENTS_LOCK),
        shown: List<NiumiPermission> = NiumiPermission.entries,
        dialog: PermissionDialog? = null,
    ) {
        val state =
            PermissionsUiState(
                tiles = shown.map { PermissionTileState(it, it in granted) },
                isLoading = false,
                dialog = dialog,
            )
        composeRule.setContent {
            NiumiTheme(dark = false) {
                PermissionsScreen(
                    state = state,
                    actions =
                        PermissionsActions(
                            onBack = { calls += "back" },
                            onAuthorize = { calls += "authorize:$it" },
                            onContinue = { calls += "continue" },
                            onDismissDialog = { calls += "dismiss" },
                            onOpenAccessibilitySettings = { calls += "accessibility" },
                            onConfirmRecentsLock = { calls += "recents" },
                        ),
                )
            }
        }
    }

    @Test
    fun theHeaderAndTheFiveTilesAreShown() {
        setContent()

        listOf(
            PermissionsTexts.title(tileCount = 5),
            PermissionsTexts.BODY,
            PermissionsTexts.ALARM_NAME,
            PermissionsTexts.ALARM_PHRASE,
            PermissionsTexts.BLOCKING_NAME,
            PermissionsTexts.BLOCKING_PHRASE,
            PermissionsTexts.BATTERY_NAME,
            PermissionsTexts.BATTERY_PHRASE,
            PermissionsTexts.RECENTS_LOCK_NAME,
            PermissionsTexts.RECENTS_LOCK_PHRASE,
            PermissionsTexts.NOTIFICATIONS_NAME,
            PermissionsTexts.NOTIFICATIONS_PHRASE,
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertExists() }
    }

    @Test
    fun withoutRecentsLockTheTitleCountsFourTiles() {
        setContent(shown = NiumiPermission.entries - NiumiPermission.RECENTS_LOCK)

        composeRule.onNodeWithText(PermissionsTexts.title(tileCount = 4)).assertExists()
        composeRule.onAllNodesWithText(PermissionsTexts.RECENTS_LOCK_NAME).assertCountEquals(0)
    }

    @Test
    fun aGrantedTileShowsTheCheckInsteadOfAuthorize() {
        setContent()

        composeRule.onAllNodesWithContentDescription(PermissionsTexts.GRANTED).assertCountEquals(3)
        // Coche à l'encre dans une pastille ambre de 28 (décision de Mehdi du 2026-10-22).
        composeRule
            .onAllNodesWithContentDescription(PermissionsTexts.GRANTED)[0]
            .assertWidthIsEqualTo(NiumiDimens.grantedBadge)
            .assertHeightIsEqualTo(NiumiDimens.grantedBadge)
        composeRule.onAllNodesWithText(PermissionsTexts.AUTHORIZE).assertCountEquals(2)
    }

    @Test
    fun continueIsInactiveWhileAPermissionIsMissing() {
        setContent(granted = NiumiPermission.entries.toSet() - NiumiPermission.NOTIFICATIONS)

        composeRule.onNodeWithText(PermissionsTexts.CONTINUE).assertIsNotEnabled()
        composeRule.onNodeWithText(PermissionsTexts.CONTINUE).performClick()
        assertThat(calls).isEmpty()
    }

    @Test
    fun continueBecomesActiveWithEveryPermission() {
        setContent(granted = NiumiPermission.entries.toSet())

        composeRule.onAllNodesWithText(PermissionsTexts.AUTHORIZE).assertCountEquals(0)
        composeRule.onNodeWithText(PermissionsTexts.CONTINUE).assertIsEnabled().performClick()
        assertThat(calls).containsExactly("continue")
    }

    @Test
    fun eachAuthorizeButtonCallsItsOwnPermissionOnly() {
        setContent()

        // Cinq tuiles peuvent dépasser la hauteur de l'écran : le contenu défile.
        composeRule.onAllNodesWithText(PermissionsTexts.AUTHORIZE)[1].performScrollTo().performClick()
        composeRule.onAllNodesWithText(PermissionsTexts.AUTHORIZE)[0].performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(PermissionsTexts.BACK).performClick()

        assertThat(calls)
            .containsExactly("authorize:NOTIFICATIONS", "authorize:APP_BLOCKING", "back")
            .inOrder()
    }

    /** SPEC_ANDROID §12.3 : les cinq points avant tout accès aux réglages, dans la fenêtre. */
    @Test
    fun theConsentWindowShowsTheFivePointsBeforeTheSettingsButton() {
        setContent(dialog = PermissionDialog.CONSENT)

        composeRule.onNodeWithText(AccessibilityConsentTexts.TITLE).assertExists()
        AccessibilityConsentTexts.points.forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithText(AccessibilityConsentTexts.HOW_TO).assertExists()
        composeRule.onNodeWithText(AccessibilityConsentTexts.OPEN_SETTINGS_BUTTON_LABEL).performClick()

        assertThat(calls).containsExactly("accessibility")
    }

    @Test
    fun theCrossOfAWindowOnlyCloses() {
        setContent(dialog = PermissionDialog.CONSENT)

        composeRule.onNodeWithContentDescription(PermissionsTexts.CLOSE).performClick()
        composeRule.waitForIdle()

        assertThat(calls).containsExactly("dismiss")
    }

    @Test
    fun theRecentsLockWindowShowsTheSpecTextAndItsButton() {
        setContent(dialog = PermissionDialog.RECENTS_LOCK)

        // Le titre reprend le nom de la tuile : on le cherche par son rôle d'intertitre.
        composeRule.onNode(hasText(PermissionsTexts.RECENTS_LOCK_NAME) and isHeading()).assertExists()
        composeRule.onNodeWithText(ReadinessMessages.forCheck(ReadinessCheckId.RECENTS_LOCK)).assertExists()
        composeRule.onNodeWithText(ReadinessMessages.actionLabelFor(ReadinessCheckId.RECENTS_LOCK)).performClick()
        composeRule.waitForIdle()

        assertThat(calls).containsExactly("recents")
    }

    @Test
    fun theExactAlarmWindowOnlyExplains() {
        setContent(
            granted = setOf(NiumiPermission.BATTERY, NiumiPermission.RECENTS_LOCK),
            dialog = PermissionDialog.EXACT_ALARM,
        )

        composeRule.onNode(hasText(PermissionsTexts.ALARM_NAME) and isHeading()).assertExists()
        composeRule.onNodeWithText(ReadinessMessages.EXACT_ALARM_DIAGNOSTIC).assertExists()
        // Rien à faire : aucun bouton, la croix seule ferme.
        composeRule.onNodeWithContentDescription(PermissionsTexts.CLOSE).assertExists()
        composeRule
            .onAllNodesWithText(
                ReadinessMessages.actionLabelFor(ReadinessCheckId.EXACT_ALARM),
            ).assertCountEquals(0)
    }

    @Test
    fun theScreenPassesTheAccessibilityChecks() {
        setContent()
        composeRule.enableAccessibilityChecks()

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theConsentWindowPassesTheAccessibilityChecks() {
        setContent(dialog = PermissionDialog.CONSENT)
        composeRule.enableAccessibilityChecks()

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
