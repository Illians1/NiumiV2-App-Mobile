package com.niumi.feature.setup.readiness

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.system.readiness.ReadinessAction
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §13 : un recours sans `Intent` système doit afficher une explication plutôt que
 * rien. La traduction en `Intent` a rejoint `:core:system` à l'étape 16 avec `settingsIntentFor` —
 * ses gardes (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM` et exemption d'énergie jamais produites) sont
 * désormais dans `ReadinessSettingsIntentsTest`. Ce qui reste ici est ce qui reste dans ce module :
 * les textes.
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
    fun onlyTheTwoActionsWithoutAnySystemRecourseCarryAnExplanation() {
        val explained = allActions.filter { explanationFor(it) != null }

        assertThat(explained)
            .containsExactly(ReadinessAction.ShowExactAlarmDiagnostic, ReadinessAction.Unsupported)
    }
}
