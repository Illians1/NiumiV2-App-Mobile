package com.niumi.system.readiness

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Garde de SPEC_ANDROID §13 et §14 : Niumi déclare `USE_EXACT_ALARM` et ne doit **jamais** rediriger
 * vers « Alarmes et rappels ». Le test énumère toutes les actions et vérifie qu'aucune ne produit
 * cette redirection, ni aucune demande d'exemption d'énergie directe — celle-ci exigerait la
 * permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, restreinte par Google Play et non déclarée.
 *
 * Déplacé de `:feature:setup` à l'étape 16 avec `settingsIntentFor` : l'écran 7 consomme désormais
 * la même traduction (§15, remédiation des incidents). La partie « explication » du test reste
 * dans `:feature:setup`, avec les textes.
 */
@RunWith(AndroidJUnit4::class)
class ReadinessSettingsIntentsTest {
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

    /**
     * L'action de remédiation minimale imposée par §15 pour `BLOCKING_PERMISSION_REVOKED` ouvre
     * bien les réglages d'accessibilité, et Niumi n'y réactive rien lui-même (§12.2).
     */
    @Test
    fun theAccessibilityRecourseOpensTheAccessibilitySettings() {
        assertThat(settingsIntentFor(ReadinessAction.OpenAccessibilitySettings, PACKAGE)?.action)
            .isEqualTo(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    private companion object {
        const val PACKAGE = "com.niumi.app"
    }
}
