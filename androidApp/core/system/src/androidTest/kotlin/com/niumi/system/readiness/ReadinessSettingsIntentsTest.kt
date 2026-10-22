package com.niumi.system.readiness

import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Garde de SPEC_ANDROID §13 et §14 : Niumi déclare `USE_EXACT_ALARM` et ne doit **jamais** rediriger
 * vers « Alarmes et rappels ». Le test énumère toutes les actions et vérifie qu'aucune ne produit
 * cette redirection. Depuis le 2026-10-21, l'exemption d'énergie est demandée directement
 * (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, déclarée), par le seul recours de batterie.
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
            ReadinessAction.RequestNotificationPermission,
            ReadinessAction.OpenChannelSettings("niumi_alarm_ringing"),
            ReadinessAction.OpenSoundSettings,
            ReadinessAction.OpenDndSettings,
            ReadinessAction.OpenAccessibilitySettings,
            ReadinessAction.FixTime,
            ReadinessAction.OpenBatterySettings,
            ReadinessAction.Unsupported,
        )

    @Test
    fun noActionEverOpensTheExactAlarmPermissionScreen() {
        allActions.forEach { action ->
            assertThat(settingsIntentFor(action, PACKAGE)?.action)
                .isNotEqualTo(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        }
    }

    /**
     * Décision de Mehdi du 2026-10-21 : la liste des optimisations demande trois touchers sur
     * Android 16 (liste, fiche de l'app, « Sans restriction »). La demande directe n'en demande
     * qu'un, et ne vise que Niumi.
     */
    @Test
    fun theBatteryRecourseRequestsTheExemptionForNiumiDirectly() {
        val intent = settingsIntentFor(ReadinessAction.OpenBatterySettings, PACKAGE)

        assertThat(intent?.action).isEqualTo(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        assertThat(intent?.data).isEqualTo(Uri.fromParts("package", PACKAGE, null))
    }

    @Test
    fun onlyTheBatteryRecourseRequestsAnExemption() {
        allActions.filter { it != ReadinessAction.OpenBatterySettings }.forEach { action ->
            assertThat(settingsIntentFor(action, PACKAGE)?.action)
                .isNotEqualTo(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        }
    }

    /** Un constructeur qui retire la fenêtre de demande garde la liste système : le recours tient. */
    @Test
    fun theBatteryRecourseFallsBackToTheSystemList() {
        assertThat(fallbackSettingsIntentFor(ReadinessAction.OpenBatterySettings)?.action)
            .isEqualTo(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        allActions.filter { it != ReadinessAction.OpenBatterySettings }.forEach { action ->
            assertThat(fallbackSettingsIntentFor(action)).isNull()
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
