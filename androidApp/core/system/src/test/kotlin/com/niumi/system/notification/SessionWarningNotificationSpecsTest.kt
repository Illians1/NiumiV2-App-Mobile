package com.niumi.system.notification

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import com.niumi.system.readiness.MonitoredReadinessChecks
import com.niumi.system.readiness.ReadinessCheckId
import org.junit.Test

/**
 * Canal et notification d'avertissement de SPEC_ANDROID §13.1 : importance haute, sans son ni
 * vibration, sans plein écran, `setOngoing(false)`, et un texte qui nomme le réglage en cause
 * ainsi que sa conséquence.
 */
class SessionWarningNotificationSpecsTest {
    @Test
    fun theWarningChannelIsHighImportanceAndCompletelySilent() {
        val spec = NiumiNotificationChannels.sessionWarning

        assertThat(spec.id).isEqualTo("niumi_session_warning")
        assertThat(spec.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(spec.hasSound).isFalse()
        assertThat(spec.hasVibration).isFalse()
    }

    @Test
    fun theThreeChannelsAreRegisteredWithDistinctIds() {
        assertThat(NiumiNotificationChannels.all).hasSize(3)
        assertThat(NiumiNotificationChannels.all.map { it.id }.toSet()).hasSize(3)
        assertThat(NiumiNotificationChannels.all).contains(NiumiNotificationChannels.sessionWarning)
    }

    @Test
    fun everyMonitoredCheckHasItsOwnWarningText() {
        MonitoredReadinessChecks.incidentCodes.keys.forEach { id ->
            val spec = SessionWarningNotificationSpecs.forCheck(id)

            assertThat(spec.channelId).isEqualTo(NiumiNotificationChannels.sessionWarning.id)
            assertThat(spec.text).isNotEmpty()
            assertThat(spec.ongoing).isFalse()
            assertThat(spec.hasFullScreenIntent).isFalse()
            assertThat(spec.actions).isEmpty()
        }
    }

    @Test
    fun theWarningTextsAreAllDistinctSoTheUserKnowsWhichSettingIsAtFault() {
        val texts =
            MonitoredReadinessChecks.incidentCodes.keys.map {
                SessionWarningNotificationSpecs
                    .forCheck(
                        it,
                    ).text
            }

        assertThat(texts.toSet()).hasSize(texts.size)
    }

    @Test
    fun eachMonitoredCheckCarriesItsOwnNotificationIdSoWarningsDoNotOverwriteEachOther() {
        val ids = MonitoredReadinessChecks.incidentCodes.keys.map { SessionWarningNotificationSpecs.notificationId(it) }

        assertThat(ids.toSet()).hasSize(ids.size)
        // 1 = sonnerie (RingingNotificationFactory), 2 = demande de scan.
        assertThat(ids.min()).isAtLeast(3)
    }

    @Test
    fun onlyTheSixChecksOfTheSpecTableAreMonitored() {
        assertThat(MonitoredReadinessChecks.incidentCodes.keys)
            .containsExactly(
                ReadinessCheckId.EXACT_ALARM,
                ReadinessCheckId.FULL_SCREEN_INTENT,
                ReadinessCheckId.NOTIFICATIONS,
                ReadinessCheckId.ALARM_VOLUME,
                ReadinessCheckId.DND_TOTAL_SILENCE,
                ReadinessCheckId.ACCESSIBILITY_SERVICE,
            )
    }
}
