package com.niumi.system.readiness

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.notification.NiumiNotificationChannels
import com.niumi.system.readiness.fakes.ReadinessTestSources
import com.niumi.system.session.fakes.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val NOW = 1_757_000_000_000L
private const val ANDROID_10 = 29
private const val ANDROID_12 = 31
private const val ANDROID_13 = 33
private const val ANDROID_14 = 34
private const val ANDROID_16 = 36

/**
 * Une assertion par ligne du tableau de SPEC_ANDROID §13, plus les cas mesurés sur appareil :
 * silence total bloquant (étape 6), exemption d'énergie non satisfaite tant que l'utilisateur
 * n'a pas confirmé (étape 5).
 */
class AndroidDeviceReadinessCheckerTest {
    private fun checker(
        sources: ReadinessTestSources,
        sdkInt: Int = ANDROID_16,
    ) = AndroidDeviceReadinessChecker(sources.build(), FakeClock(NOW), sdkInt)

    private suspend fun report(
        sources: ReadinessTestSources,
        sdkInt: Int = ANDROID_16,
        candidateTriggerAtEpochMillis: Long? = null,
    ) = checker(sources, sdkInt).check(ReadinessInput(candidateTriggerAtEpochMillis))

    @Test
    fun everyApplicableCheckPassesWhenAllSourcesAreReady() =
        runTest {
            val report = report(ReadinessTestSources())

            assertThat(report.checks.map { it.id }).containsExactlyElementsIn(ReadinessCheckId.entries).inOrder()
            assertThat(report.checks.filter { it.outcome == ReadinessOutcome.FAILED }).isEmpty()
            assertThat(report.check(ReadinessCheckId.FUTURE_TRIGGER).outcome)
                .isEqualTo(ReadinessOutcome.NOT_APPLICABLE)
            assertThat(report.firstFailure()).isNull()
        }

    @Test
    fun missingNfcHardwareBlocksTheNiumiExperienceWithNoRecourse() =
        runTest {
            val sources = ReadinessTestSources()
            sources.nfcReader.availabilityValue = NfcAvailability.ABSENT

            val report = report(sources)

            val present = report.check(ReadinessCheckId.NFC_PRESENT)
            assertThat(present.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(present.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE)
            assertThat(present.action).isEqualTo(ReadinessAction.Unsupported)
            // Proposer « active le NFC » sur un appareil qui n'en a pas serait un faux recours.
            assertThat(report.check(ReadinessCheckId.NFC_ENABLED).outcome)
                .isEqualTo(ReadinessOutcome.NOT_APPLICABLE)
        }

    @Test
    fun disabledNfcProposesTheNfcSettings() =
        runTest {
            val sources = ReadinessTestSources()
            sources.nfcReader.availabilityValue = NfcAvailability.DISABLED

            val report = report(sources)

            assertThat(report.check(ReadinessCheckId.NFC_PRESENT).outcome).isEqualTo(ReadinessOutcome.PASSED)
            val enabled = report.check(ReadinessCheckId.NFC_ENABLED)
            assertThat(enabled.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(enabled.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE)
            assertThat(enabled.action).isEqualTo(ReadinessAction.OpenNfcSettings)
        }

    @Test
    fun noPairedBoxBlocksAndProposesPairing() =
        runTest {
            val sources = ReadinessTestSources()
            sources.pairedBoxStore.clear()

            val report = report(sources)

            val check = report.check(ReadinessCheckId.PAIRED_BOX)
            assertThat(check.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(check.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE)
            assertThat(check.action).isEqualTo(ReadinessAction.StartPairing)
            assertThat(report.hasPairedBox).isFalse()
        }

    @Test
    fun anEmptyOrOversizedAppSelectionBlocksAndProposesThePicker() =
        runTest {
            val sources = ReadinessTestSources()
            sources.appSelectionSource.count = 0

            val empty = report(sources)
            assertThat(empty.check(ReadinessCheckId.APP_SELECTION).outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(empty.check(ReadinessCheckId.APP_SELECTION).action).isEqualTo(ReadinessAction.OpenAppPicker)
            assertThat(empty.appSelectionCount).isEqualTo(0)

            sources.appSelectionSource.count = 51
            assertThat(report(sources).check(ReadinessCheckId.APP_SELECTION).outcome)
                .isEqualTo(ReadinessOutcome.FAILED)

            sources.appSelectionSource.count = 50
            assertThat(report(sources).check(ReadinessCheckId.APP_SELECTION).outcome)
                .isEqualTo(ReadinessOutcome.PASSED)
        }

    @Test
    fun lostExactAlarmAccessIsBlockingForAlarmAndNeverRedirectsToAlarmsAndReminders() =
        runTest {
            val sources = ReadinessTestSources()
            sources.alarmScheduler.canScheduleExactValue = false

            val report = report(sources)

            val check = report.check(ReadinessCheckId.EXACT_ALARM)
            assertThat(check.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(check.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_ALARM)
            // §13, §14 : Niumi déclare USE_EXACT_ALARM, jamais SCHEDULE_EXACT_ALARM. Un échec est
            // un état anormal, pas une permission à demander.
            assertThat(check.action).isEqualTo(ReadinessAction.ShowExactAlarmDiagnostic)
        }

    @Test
    fun fullScreenIntentIsNotApplicableBeforeAndroid14() =
        runTest {
            val sources = ReadinessTestSources()
            sources.notificationAvailability.fullScreenAllowed = false

            listOf(ANDROID_10, ANDROID_12, ANDROID_13).forEach { sdkInt ->
                assertThat(report(sources, sdkInt).check(ReadinessCheckId.FULL_SCREEN_INTENT).outcome)
                    .isEqualTo(ReadinessOutcome.NOT_APPLICABLE)
            }
        }

    @Test
    fun deniedFullScreenIntentFromAndroid14BlocksTheNiumiExperience() =
        runTest {
            val sources = ReadinessTestSources()
            sources.notificationAvailability.fullScreenAllowed = false

            val check = report(sources, ANDROID_14).check(ReadinessCheckId.FULL_SCREEN_INTENT)

            assertThat(check.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(check.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE)
            assertThat(check.action).isEqualTo(ReadinessAction.OpenFullScreenIntentSettings)
        }

    @Test
    fun revokedNotificationsAskForThePermissionFromAndroid13AndForTheChannelSettingsBelow() =
        runTest {
            val sources = ReadinessTestSources()
            sources.notificationAvailability.notificationsEnabled = false

            val modern = report(sources, ANDROID_13).check(ReadinessCheckId.NOTIFICATIONS)
            assertThat(modern.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(modern.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE)
            assertThat(modern.action).isEqualTo(ReadinessAction.RequestNotificationPermission)

            // Avant Android 13, POST_NOTIFICATIONS n'existe pas : rien à demander, seul le
            // réglage manuel du canal reste ouvert.
            val legacy = report(sources, ANDROID_10).check(ReadinessCheckId.NOTIFICATIONS)
            assertThat(legacy.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(legacy.action)
                .isEqualTo(ReadinessAction.OpenChannelSettings(NiumiNotificationChannels.alarmRinging.id))
        }

    @Test
    fun aTurnedOffAlarmChannelProposesTheChannelSettings() =
        runTest {
            val sources = ReadinessTestSources()
            sources.notificationChannelStatus.enabledChannels.remove(NiumiNotificationChannels.alarmRinging.id)

            val check = report(sources).check(ReadinessCheckId.ALARM_CHANNEL)

            assertThat(check.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(check.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE)
            assertThat(check.action)
                .isEqualTo(ReadinessAction.OpenChannelSettings(NiumiNotificationChannels.alarmRinging.id))
        }

    @Test
    fun anAlarmVolumeAtZeroIsBlockingForAlarm() =
        runTest {
            val sources = ReadinessTestSources()
            sources.alarmVolumeSource.volume = 0

            val check = report(sources).check(ReadinessCheckId.ALARM_VOLUME)

            assertThat(check.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(check.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_ALARM)
            assertThat(check.action).isEqualTo(ReadinessAction.OpenSoundSettings)
        }

    @Test
    fun totalSilenceIsBlockingForAlarmAndProposesTheDndSettings() =
        runTest {
            val sources = ReadinessTestSources()
            sources.interruptionFilterSource.filter = NotificationManager.INTERRUPTION_FILTER_NONE

            val report = report(sources)

            val silence = report.check(ReadinessCheckId.DND_TOTAL_SILENCE)
            assertThat(silence.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(silence.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_ALARM)
            assertThat(silence.action).isEqualTo(ReadinessAction.OpenDndSettings)
            // Le silence total n'est pas « un autre mode » : une seule cause, un seul contrôle
            // en échec, sinon l'écran afficherait deux fois le même problème.
            assertThat(report.check(ReadinessCheckId.DND_OTHER_MODE).outcome).isEqualTo(ReadinessOutcome.PASSED)
        }

    @Test
    fun theOtherInterruptionFiltersAreOnlyAWarning() =
        runTest {
            val sources = ReadinessTestSources()
            listOf(
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                NotificationManager.INTERRUPTION_FILTER_ALARMS,
            ).forEach { filter ->
                sources.interruptionFilterSource.filter = filter

                val report = report(sources)

                val other = report.check(ReadinessCheckId.DND_OTHER_MODE)
                assertThat(other.outcome).isEqualTo(ReadinessOutcome.FAILED)
                assertThat(other.severity).isEqualTo(ReadinessSeverityDto.WARNING)
                assertThat(other.action).isEqualTo(ReadinessAction.OpenDndSettings)
                assertThat(report.check(ReadinessCheckId.DND_TOTAL_SILENCE).outcome)
                    .isEqualTo(ReadinessOutcome.PASSED)
            }
        }

    @Test
    fun aDisabledAccessibilityServiceBlocksTheNiumiExperience() =
        runTest {
            val sources = ReadinessTestSources()
            sources.accessibilityServiceStatus.enabled = false

            val check = report(sources).check(ReadinessCheckId.ACCESSIBILITY_SERVICE)

            assertThat(check.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(check.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE)
            assertThat(check.action).isEqualTo(ReadinessAction.OpenAccessibilitySettings)
        }

    @Test
    fun theTriggerInstantIsCheckedOnlyOnceAWakeTimeHasBeenChosen() =
        runTest {
            val sources = ReadinessTestSources()

            assertThat(report(sources).check(ReadinessCheckId.FUTURE_TRIGGER).outcome)
                .isEqualTo(ReadinessOutcome.NOT_APPLICABLE)

            val past = report(sources, candidateTriggerAtEpochMillis = NOW).check(ReadinessCheckId.FUTURE_TRIGGER)
            assertThat(past.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(past.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_ALARM)
            assertThat(past.action).isEqualTo(ReadinessAction.FixTime)

            assertThat(
                report(sources, candidateTriggerAtEpochMillis = NOW + 1)
                    .check(ReadinessCheckId.FUTURE_TRIGGER)
                    .outcome,
            ).isEqualTo(ReadinessOutcome.PASSED)
        }

    @Test
    fun theBatteryExemptionStaysUnsatisfiedUntilTheUserConfirmsItEvenWhenAndroidReportsItGranted() =
        runTest {
            val sources = ReadinessTestSources()
            sources.batteryOptimizationStatus.ignoring = true
            sources.setupPreferences.batteryExemptionConfirmed = false

            val check = report(sources).check(ReadinessCheckId.BATTERY_OPTIMIZATION)

            assertThat(check.outcome).isEqualTo(ReadinessOutcome.FAILED)
            assertThat(check.severity).isEqualTo(ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE)
            assertThat(check.action).isEqualTo(ReadinessAction.OpenBatterySettings(aospExemptionGranted = true))
        }

    @Test
    fun theBatteryActionCarriesTheAospExemptionStateSoTheScreenCanGuideTowardsTheOemSetting() =
        runTest {
            val sources = ReadinessTestSources()
            sources.batteryOptimizationStatus.ignoring = false
            sources.setupPreferences.batteryExemptionConfirmed = false

            val check = report(sources).check(ReadinessCheckId.BATTERY_OPTIMIZATION)

            assertThat(check.action).isEqualTo(ReadinessAction.OpenBatterySettings(aospExemptionGranted = false))
        }

    @Test
    fun theFirstFailureFollowsTheOrderOfTheSpecTable() =
        runTest {
            val sources = ReadinessTestSources()
            sources.accessibilityServiceStatus.enabled = false
            sources.nfcReader.availabilityValue = NfcAvailability.DISABLED

            val report = report(sources)

            assertThat(report.firstFailure()?.id).isEqualTo(ReadinessCheckId.NFC_ENABLED)
        }
}
