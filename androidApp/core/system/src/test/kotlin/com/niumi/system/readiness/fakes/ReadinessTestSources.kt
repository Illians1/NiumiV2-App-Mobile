package com.niumi.system.readiness.fakes

import android.app.Activity
import android.app.NotificationManager
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionSource
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.common.OperationResult
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.notification.InterruptionFilterSource
import com.niumi.system.notification.NiumiNotificationChannels
import com.niumi.system.notification.NotificationAvailability
import com.niumi.system.notification.NotificationChannelStatus
import com.niumi.system.power.BatteryOptimizationStatus
import com.niumi.system.readiness.ReadinessSources
import com.niumi.system.session.fakes.FakeAccessibilityServiceStatus
import com.niumi.system.session.fakes.FakeAlarmScheduler
import com.niumi.system.setup.SetupPreferences

class FakeNfcReader(
    var availabilityValue: NfcAvailability = NfcAvailability.ENABLED,
) : NfcReader {
    override fun start(
        activity: Activity,
        onUri: (String) -> Unit,
        onUnreadable: () -> Unit,
    ): OperationResult = OperationResult.Success

    override fun stop(activity: Activity) = Unit

    override val availability: NfcAvailability
        get() = availabilityValue
}

class FakePairedBoxStore(
    private var credential: PairedBoxCredentialDto? = PairedBoxCredentialDto(1, "box-1", "a".repeat(64)),
) : PairedBoxStore {
    override suspend fun current(): PairedBoxCredentialDto? = credential

    override suspend fun replace(credential: PairedBoxCredentialDto) {
        this.credential = credential
    }

    override suspend fun clear() {
        credential = null
    }
}

class FakeAppSelectionSource(
    var count: Int = 3,
) : AppSelectionSource {
    override suspend fun selectedCount(): Int = count
}

class FakeNotificationAvailability(
    var notificationsEnabled: Boolean = true,
    var fullScreenAllowed: Boolean = true,
) : NotificationAvailability {
    override fun areNotificationsEnabled(): Boolean = notificationsEnabled

    override fun canUseFullScreenIntent(): Boolean = fullScreenAllowed
}

class FakeNotificationChannelStatus(
    var enabledChannels: MutableSet<String> =
        mutableSetOf(
            NiumiNotificationChannels.alarmRinging.id,
            NiumiNotificationChannels.sessionAwaitingScan.id,
        ),
) : NotificationChannelStatus {
    override fun isChannelEnabled(channelId: String): Boolean = channelId in enabledChannels
}

class FakeAlarmVolumeSource(
    var volume: Int = 7,
) : AlarmVolumeSource {
    override fun alarmStreamVolume(): Int = volume
}

class FakeInterruptionFilterSource(
    var filter: Int = NotificationManager.INTERRUPTION_FILTER_ALL,
) : InterruptionFilterSource {
    override fun currentInterruptionFilter(): Int = filter
}

class FakeBatteryOptimizationStatus(
    var ignoring: Boolean = true,
) : BatteryOptimizationStatus {
    override fun isIgnoringBatteryOptimizations(): Boolean = ignoring
}

class FakeSetupPreferences(
    var onboardingAcknowledged: Boolean = true,
    var batteryExemptionConfirmed: Boolean = true,
    var lastWakeTimeIsoValue: String? = null,
) : SetupPreferences {
    override suspend fun isOnboardingAcknowledged(): Boolean = onboardingAcknowledged

    override suspend fun acknowledgeOnboarding() {
        onboardingAcknowledged = true
    }

    override suspend fun isBatteryExemptionConfirmed(): Boolean = batteryExemptionConfirmed

    override suspend fun setBatteryExemptionConfirmed(confirmed: Boolean) {
        batteryExemptionConfirmed = confirmed
    }

    override suspend fun lastWakeTimeIso(): String? = lastWakeTimeIsoValue

    override suspend fun setLastWakeTimeIso(value: String) {
        lastWakeTimeIsoValue = value
    }
}

/**
 * Assemble des sources toutes vertes : chaque test ne dégrade que la source qu'il examine,
 * ce qui garde une assertion par ligne du tableau de SPEC_ANDROID §13 lisible.
 */
class ReadinessTestSources(
    // Partagés avec `TestCoordinatorHarness` quand le moniteur doit voir les mêmes fakes que le
    // reste du coordinateur : sans cela, `harness.alarmScheduler.canScheduleExactValue = false`
    // n'atteindrait jamais le diagnostic.
    val alarmScheduler: FakeAlarmScheduler = FakeAlarmScheduler(),
    val accessibilityServiceStatus: FakeAccessibilityServiceStatus = FakeAccessibilityServiceStatus(),
) {
    val nfcReader = FakeNfcReader()
    val pairedBoxStore = FakePairedBoxStore()
    val appSelectionSource = FakeAppSelectionSource()
    val notificationAvailability = FakeNotificationAvailability()
    val notificationChannelStatus = FakeNotificationChannelStatus()
    val alarmVolumeSource = FakeAlarmVolumeSource()
    val interruptionFilterSource = FakeInterruptionFilterSource()
    val batteryOptimizationStatus = FakeBatteryOptimizationStatus()
    val setupPreferences = FakeSetupPreferences()

    fun build(): ReadinessSources =
        ReadinessSources(
            nfcReader = nfcReader,
            pairedBoxStore = pairedBoxStore,
            appSelectionSource = appSelectionSource,
            alarmScheduler = alarmScheduler,
            notificationAvailability = notificationAvailability,
            notificationChannelStatus = notificationChannelStatus,
            alarmVolumeSource = alarmVolumeSource,
            interruptionFilterSource = interruptionFilterSource,
            accessibilityServiceStatus = accessibilityServiceStatus,
            batteryOptimizationStatus = batteryOptimizationStatus,
            setupPreferences = setupPreferences,
        )
}
