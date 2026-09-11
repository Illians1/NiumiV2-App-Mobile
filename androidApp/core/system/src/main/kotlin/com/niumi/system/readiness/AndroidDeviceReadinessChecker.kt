package com.niumi.system.readiness

import android.app.NotificationManager
import android.os.Build
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.system.common.Clock
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.notification.NiumiNotificationChannels

/**
 * Implémentation Android du tableau de SPEC_ANDROID §13. Ne décide jamais si l'activation est
 * permise : elle décrit l'état de l'appareil, [ReadinessDtoMapper] le convertit et
 * `NiumiCoreFacade.evaluateActivation` tranche (§13, premier alinéa).
 *
 * [sdkInt] est injecté plutôt que lu depuis `Build.VERSION` : les branches Android 13 et 14 sont
 * ainsi vérifiables en test JVM, où `Build.VERSION.SDK_INT` vaut 0.
 */
class AndroidDeviceReadinessChecker(
    private val sources: ReadinessSources,
    private val clock: Clock,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : DeviceReadinessChecker {
    override suspend fun check(input: ReadinessInput): ReadinessReport {
        val now = clock.nowEpochMillis()
        val availability = sources.nfcReader.availability
        val hasPairedBox = sources.pairedBoxStore.current() != null
        val appSelectionCount = sources.appSelectionSource.selectedCount()
        val interruptionFilter = sources.interruptionFilterSource.currentInterruptionFilter()

        return ReadinessReport(
            checks =
                nfcChecks(availability) +
                    journeyChecks(hasPairedBox, appSelectionCount) +
                    deliveryChecks() +
                    audibilityChecks(interruptionFilter) +
                    sessionChecks(input.candidateTriggerAtEpochMillis, now),
            appSelectionCount = appSelectionCount,
            hasPairedBox = hasPairedBox,
            candidateTriggerAtEpochMillis = input.candidateTriggerAtEpochMillis,
            nowEpochMillis = now,
        )
    }

    /** Contrôles 1 et 2 : proposer d'activer le NFC sur un appareil qui n'en a pas serait faux. */
    private fun nfcChecks(availability: NfcAvailability): List<ReadinessCheck> =
        listOf(
            check(
                ReadinessCheckId.NFC_PRESENT,
                ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                passed = availability != NfcAvailability.ABSENT,
                action = ReadinessAction.Unsupported,
            ),
            if (availability == NfcAvailability.ABSENT) {
                notApplicable(
                    ReadinessCheckId.NFC_ENABLED,
                    ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                    ReadinessAction.Unsupported,
                )
            } else {
                check(
                    ReadinessCheckId.NFC_ENABLED,
                    ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                    passed = availability == NfcAvailability.ENABLED,
                    action = ReadinessAction.OpenNfcSettings,
                )
            },
        )

    /**
     * Contrôles 3 et 4. Affichés comme les autres, mais convertis par [ReadinessDtoMapper] vers
     * `hasPairedBox` et `appSelectionCount` : la politique commune les refuse déjà avec
     * `NO_PAIRED_BOX` et `INVALID_APP_SELECTION`, les verser aussi dans `checks` doublerait le
     * refus. Les bornes 1..50 viennent de `:shared:core`, jamais réécrites ici.
     */
    private fun journeyChecks(
        hasPairedBox: Boolean,
        appSelectionCount: Int,
    ): List<ReadinessCheck> =
        listOf(
            check(
                ReadinessCheckId.PAIRED_BOX,
                ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                passed = hasPairedBox,
                action = ReadinessAction.StartPairing,
            ),
            check(
                ReadinessCheckId.APP_SELECTION,
                ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                passed = appSelectionCount in AppSelectionSummary.MIN_COUNT..AppSelectionSummary.MAX_COUNT,
                action = ReadinessAction.OpenAppPicker,
            ),
        )

    /** Contrôles 5 à 8 : la chaîne qui amène le réveil jusqu'à l'écran de scan. */
    private fun deliveryChecks(): List<ReadinessCheck> =
        listOf(
            check(
                ReadinessCheckId.EXACT_ALARM,
                ReadinessSeverityDto.BLOCKING_FOR_ALARM,
                passed = sources.alarmScheduler.canScheduleExact(),
                // Jamais d'action vers « Alarmes et rappels » : Niumi ne déclare pas
                // SCHEDULE_EXACT_ALARM, un échec signale un état anormal (§13, §14).
                action = ReadinessAction.ShowExactAlarmDiagnostic,
            ),
            if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                check(
                    ReadinessCheckId.FULL_SCREEN_INTENT,
                    ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                    passed = sources.notificationAvailability.canUseFullScreenIntent(),
                    action = ReadinessAction.OpenFullScreenIntentSettings,
                )
            } else {
                // Avant Android 14, aucune autorisation distincte n'existe : rien à contrôler.
                notApplicable(
                    ReadinessCheckId.FULL_SCREEN_INTENT,
                    ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                    ReadinessAction.OpenFullScreenIntentSettings,
                )
            },
            check(
                ReadinessCheckId.NOTIFICATIONS,
                ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                passed = sources.notificationAvailability.areNotificationsEnabled(),
                // POST_NOTIFICATIONS n'existe qu'à partir d'Android 13 ; en dessous, seul le
                // réglage manuel du canal peut rétablir les notifications.
                action =
                    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                        ReadinessAction.RequestNotificationPermission
                    } else {
                        ReadinessAction.OpenChannelSettings(NiumiNotificationChannels.alarmRinging.id)
                    },
            ),
            check(
                ReadinessCheckId.ALARM_CHANNEL,
                ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                passed = sources.notificationChannelStatus.isChannelEnabled(NiumiNotificationChannels.alarmRinging.id),
                action = ReadinessAction.OpenChannelSettings(NiumiNotificationChannels.alarmRinging.id),
            ),
        )

    /**
     * Contrôles 9 à 11. Le silence total et « un autre mode » sont exclusifs : un seul des deux
     * échoue à la fois, sinon l'écran afficherait deux fois la même cause. Mesure de l'étape 6 :
     * seul `INTERRUPTION_FILTER_NONE` mute réellement `STREAM_ALARM` et supprime l'écran de
     * réveil ; `PRIORITY` et `ALARMS` laissent le parcours garanti intact.
     */
    private fun audibilityChecks(interruptionFilter: Int): List<ReadinessCheck> =
        listOf(
            check(
                ReadinessCheckId.ALARM_VOLUME,
                ReadinessSeverityDto.BLOCKING_FOR_ALARM,
                passed = sources.alarmVolumeSource.alarmStreamVolume() > 0,
                action = ReadinessAction.OpenSoundSettings,
            ),
            check(
                ReadinessCheckId.DND_TOTAL_SILENCE,
                ReadinessSeverityDto.BLOCKING_FOR_ALARM,
                passed = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_NONE,
                action = ReadinessAction.OpenDndSettings,
            ),
            check(
                ReadinessCheckId.DND_OTHER_MODE,
                ReadinessSeverityDto.WARNING,
                passed =
                    interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL ||
                        interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE,
                action = ReadinessAction.OpenDndSettings,
            ),
        )

    /** Contrôles 12 à 14. */
    private suspend fun sessionChecks(
        candidateTriggerAtEpochMillis: Long?,
        nowEpochMillis: Long,
    ): List<ReadinessCheck> =
        listOf(
            check(
                ReadinessCheckId.ACCESSIBILITY_SERVICE,
                ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                passed = sources.accessibilityServiceStatus.isEnabled(),
                action = ReadinessAction.OpenAccessibilitySettings,
            ),
            if (candidateTriggerAtEpochMillis == null) {
                // L'écran de diagnostic précède le choix de l'heure : rien à contrôler encore.
                notApplicable(
                    ReadinessCheckId.FUTURE_TRIGGER,
                    ReadinessSeverityDto.BLOCKING_FOR_ALARM,
                    ReadinessAction.FixTime,
                )
            } else {
                check(
                    ReadinessCheckId.FUTURE_TRIGGER,
                    ReadinessSeverityDto.BLOCKING_FOR_ALARM,
                    passed = candidateTriggerAtEpochMillis > nowEpochMillis,
                    action = ReadinessAction.FixTime,
                )
            },
            check(
                ReadinessCheckId.BATTERY_OPTIMIZATION,
                ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                // §13 : la détection système est partielle (sur HyperOS,
                // `isIgnoringBatteryOptimizations()` reste faux après correction du réglage OEM,
                // et vrai n'a jamais prouvé que le processus ne sera pas gelé). Le contrôle ne
                // peut donc reposer que sur la confirmation explicite de l'utilisateur ; l'état
                // AOSP ne sert qu'à choisir le recours proposé.
                passed = sources.setupPreferences.isBatteryExemptionConfirmed(),
                action =
                    ReadinessAction.OpenBatterySettings(
                        aospExemptionGranted = sources.batteryOptimizationStatus.isIgnoringBatteryOptimizations(),
                    ),
            ),
        )

    private fun check(
        id: ReadinessCheckId,
        severity: ReadinessSeverityDto,
        passed: Boolean,
        action: ReadinessAction,
    ): ReadinessCheck =
        ReadinessCheck(
            id = id,
            severity = severity,
            outcome = if (passed) ReadinessOutcome.PASSED else ReadinessOutcome.FAILED,
            action = action,
        )

    private fun notApplicable(
        id: ReadinessCheckId,
        severity: ReadinessSeverityDto,
        action: ReadinessAction,
    ): ReadinessCheck = ReadinessCheck(id, severity, ReadinessOutcome.NOT_APPLICABLE, action)
}
