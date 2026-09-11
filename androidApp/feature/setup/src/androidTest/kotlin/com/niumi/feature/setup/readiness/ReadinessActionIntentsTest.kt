package com.niumi.feature.setup.readiness

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.system.readiness.ReadinessAction
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Garde de SPEC_ANDROID §13 et §14 : Niumi déclare `USE_EXACT_ALARM` et ne doit **jamais** rediriger
 * vers « Alarmes et rappels ». Le test énumère toutes les actions et vérifie qu'aucune ne produit
 * cette redirection, ni aucune demande d'exemption d'énergie directe — celle-ci exigerait la
 * permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, restreinte par Google Play et non déclarée.
 */
@RunWith(AndroidJUnit4::class)
class ReadinessActionIntentsTest {
    private val allActions =
        listOf(
            ReadinessAction.OpenNfcSettings,
            ReadinessAction.StartPairing,
            ReadinessAction.OpenAppPicker,
            ReadinessAction.ShowExactAlarmDiagnostic,
            ReadinessAction.OpenFullScreenIntentSettings,
            ReadinessAction.RequestNotificationPermission,
            ReadinessAction.OpenChannelSettings("niumi_alarm_ringing"),
            ReadinessAction.OpenSoundSettings,
            ReadinessAction.OpenDndSettings,
            ReadinessAction.OpenAccessibilitySettings,
            ReadinessAction.FixTime,
            ReadinessAction.OpenBatterySettings(aospExemptionGranted = false),
            ReadinessAction.OpenBatterySettings(aospExemptionGranted = true),
            ReadinessAction.Unsupported,
        )

    @Test
    fun noActionEverOpensTheExactAlarmPermissionScreen() {
        allActions.forEach { action ->
            assertThat(settingsIntentFor(action, PACKAGE)?.action)
                .isNotEqualTo(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        }
    }

    @Test
    fun noActionEverRequestsTheRestrictedBatteryExemptionDirectly() {
        allActions.forEach { action ->
            assertThat(settingsIntentFor(action, PACKAGE)?.action)
                .isNotEqualTo(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        }
    }

    @Test
    fun theChannelActionCarriesBothExtrasTheSystemNeeds() {
        val intent = settingsIntentFor(ReadinessAction.OpenChannelSettings("niumi_alarm_ringing"), PACKAGE)

        assertThat(intent?.action).isEqualTo(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        assertThat(intent?.getStringExtra(Settings.EXTRA_APP_PACKAGE)).isEqualTo(PACKAGE)
        assertThat(intent?.getStringExtra(Settings.EXTRA_CHANNEL_ID)).isEqualTo("niumi_alarm_ringing")
    }

    @Test
    fun inAppRecoursesProduceNoSettingsIntent() {
        listOf(
            ReadinessAction.StartPairing,
            ReadinessAction.OpenAppPicker,
            ReadinessAction.RequestNotificationPermission,
            ReadinessAction.ShowExactAlarmDiagnostic,
            ReadinessAction.FixTime,
            ReadinessAction.Unsupported,
        ).forEach { action ->
            assertThat(settingsIntentFor(action, PACKAGE)).isNull()
        }
    }

    @Test
    fun onlyTheTwoActionsWithoutAnySystemRecourseCarryAnExplanation() {
        val explained = allActions.filter { explanationFor(it) != null }

        assertThat(explained)
            .containsExactly(ReadinessAction.ShowExactAlarmDiagnostic, ReadinessAction.Unsupported)
    }

    private companion object {
        const val PACKAGE = "com.niumi.app"
    }
}
