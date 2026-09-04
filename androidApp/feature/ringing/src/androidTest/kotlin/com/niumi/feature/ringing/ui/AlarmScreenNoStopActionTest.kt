package com.niumi.feature.ringing.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Garde-fou automatisé de SPEC_ANDROID §19.1 : aucun bouton d'arrêt sur l'écran de sonnerie.
 * Vérifié en cherchant tout nœud sémantique porteur d'une action de clic ; `AlarmScreen`
 * n'en expose aucun. Nécessite un appareil ou un émulateur (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class AlarmScreenNoStopActionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun alarmScreenExposesNoClickableNode() {
        composeRule.setContent {
            AlarmScreen(
                state = AlarmScreenState.from(AlarmRingingPhase.RINGING, deviceLocked = false),
                currentTimeText = "07:00",
            )
        }

        val clickableNodes = composeRule.onAllNodes(hasClickAction())
        assertThat(clickableNodes.fetchSemanticsNodes()).isEmpty()
    }
}
