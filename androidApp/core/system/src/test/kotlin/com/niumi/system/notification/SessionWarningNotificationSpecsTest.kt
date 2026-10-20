package com.niumi.system.notification

import android.app.NotificationManager
import android.app.PendingIntent
import com.google.common.truth.Truth.assertThat
import com.niumi.system.intent.IntentExtraValue
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.NiumiDeepLink
import com.niumi.system.intent.PendingIntentSpec
import com.niumi.system.readiness.MonitoredReadinessChecks
import com.niumi.system.readiness.ReadinessCheckId
import org.junit.Test

/**
 * Canal et notification d'avertissement de SPEC_ANDROID §13.1 : importance haute, sans son ni
 * vibration, `setOngoing(false)`, et un texte qui nomme le réglage en cause
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

    /** Quatre depuis le service de garde du blocage (SPEC_ANDROID §12.5). */
    @Test
    fun theFourChannelsAreRegisteredWithDistinctIds() {
        assertThat(NiumiNotificationChannels.all).hasSize(4)
        assertThat(NiumiNotificationChannels.all.map { it.id }.toSet()).hasSize(4)
        assertThat(NiumiNotificationChannels.all).contains(NiumiNotificationChannels.sessionWarning)
    }

    @Test
    fun everyMonitoredCheckHasItsOwnWarningText() {
        MonitoredReadinessChecks.incidentCodes.keys.forEach { id ->
            val spec = SessionWarningNotificationSpecs.forCheck(id)

            assertThat(spec.channelId).isEqualTo(NiumiNotificationChannels.sessionWarning.id)
            assertThat(spec.text).isNotEmpty()
            assertThat(spec.ongoing).isFalse()
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
                ReadinessCheckId.NOTIFICATIONS,
                ReadinessCheckId.ALARM_VOLUME,
                ReadinessCheckId.DND_TOTAL_SILENCE,
                ReadinessCheckId.ACCESSIBILITY_SERVICE,
                ReadinessCheckId.BATTERY_OPTIMIZATION,
            ).inOrder()
    }

    /**
     * Lot 9 : les identifiants ne se déduisent plus de l'ordre de surveillance. Retirer le contrôle
     * du plein écran (4) aurait décalé tous les suivants, et un avertissement publié par la version
     * antérieure n'aurait plus été retiré après la mise à jour.
     */
    @Test
    fun warningIdsAreFrozenAcrossVersions() {
        val ids =
            MonitoredReadinessChecks.incidentCodes.keys.associateWith {
                SessionWarningNotificationSpecs.notificationId(it)
            }

        assertThat(ids)
            .containsExactly(
                ReadinessCheckId.EXACT_ALARM,
                3,
                ReadinessCheckId.NOTIFICATIONS,
                5,
                ReadinessCheckId.ALARM_VOLUME,
                6,
                ReadinessCheckId.DND_TOTAL_SILENCE,
                7,
                ReadinessCheckId.ACCESSIBILITY_SERVICE,
                8,
                ReadinessCheckId.BATTERY_OPTIMIZATION,
                9,
            )
    }

    @Test
    fun theRetiredFullScreenWarningIdIsFour() {
        assertThat(SessionWarningNotificationSpecs.retiredNotificationIds).containsExactly(4)
        val current =
            MonitoredReadinessChecks.incidentCodes.keys.map {
                SessionWarningNotificationSpecs.notificationId(
                    it,
                )
            }
        assertThat(current).containsNoneIn(SessionWarningNotificationSpecs.retiredNotificationIds)
    }

    /**
     * Un avertissement « plein écran » encore affiché au moment de la mise à jour ne serait jamais
     * retiré si `clearAll()` ne visait que les contrôles surveillés aujourd'hui.
     */
    @Test
    fun clearAllAlsoWithdrawsTheRetiredFullScreenWarning() {
        assertThat(SessionWarningNotificationSpecs.idsToClear())
            .containsExactly(3, 4, 5, 6, 7, 8, 9)
    }

    /**
     * §13.1 : « Le tap de la notification ouvre `MainActivity`, qui redirige vers le diagnostic
     * d'incident. » Depuis l'étape 16, l'écran 12 existe et l'extra porte la destination.
     */
    @Test
    fun tappingAWarningCarriesTheIncidentDiagnosticDestination() {
        val spec = SessionWarningNotificationSpecs.tap(ReadinessCheckId.ACCESSIBILITY_SERVICE)

        assertThat(spec.kind).isEqualTo(PendingIntentSpec.Kind.ACTIVITY)
        assertThat(spec.target).isEqualTo(NiumiComponent.MAIN_ACTIVITY)
        assertThat(spec.extras[NiumiDeepLink.EXTRA_DESTINATION])
            .isEqualTo(IntentExtraValue.Text(NiumiDeepLink.DESTINATION_INCIDENT_DIAGNOSTIC))
    }

    /** §16 : `PendingIntent` immuables pour tous les composants internes. */
    @Test
    fun everyWarningTapIsImmutableAndKeepsItsOwnRequestCode() {
        val specs = MonitoredReadinessChecks.incidentCodes.keys.map { SessionWarningNotificationSpecs.tap(it) }

        specs.forEach { assertThat(it.flags and PendingIntent.FLAG_IMMUTABLE).isNotEqualTo(0) }
        assertThat(specs.map { it.requestCode }.toSet()).hasSize(specs.size)
    }
}
