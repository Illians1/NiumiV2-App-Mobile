package com.niumi.system.readiness.di

import android.content.Context
import android.os.Build
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.apps.AppSelectionSource
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.common.Clock
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.nfc.NfcReader
import com.niumi.system.notification.InterruptionFilterSource
import com.niumi.system.notification.NotificationAvailability
import com.niumi.system.notification.NotificationChannelStatus
import com.niumi.system.notification.SessionWarningNotifier
import com.niumi.system.power.BatteryOptimizationStatus
import com.niumi.system.readiness.AndroidDeviceReadinessChecker
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessSources
import com.niumi.system.readiness.SessionReadinessMonitor
import com.niumi.system.readiness.SessionReadinessWatcher
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.setup.SetupPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/** Bindings du diagnostic avant activation et de sa surveillance (SPEC_ANDROID §13, §13.1). */
@Module
@InstallIn(SingletonComponent::class)
object ReadinessModule {
    @Provides
    @Singleton
    fun provideInterruptionFilterSource(
        @ApplicationContext context: Context,
    ): InterruptionFilterSource =
        com.niumi.system.notification
            .AndroidInterruptionFilterSource(context)

    @Provides
    @Singleton
    fun provideNotificationChannelStatus(
        @ApplicationContext context: Context,
    ): NotificationChannelStatus =
        com.niumi.system.notification
            .AndroidNotificationChannelStatus(context)

    @Provides
    @Singleton
    fun provideBatteryOptimizationStatus(
        @ApplicationContext context: Context,
    ): BatteryOptimizationStatus =
        com.niumi.system.power
            .AndroidBatteryOptimizationStatus(context)

    @Provides
    @Singleton
    fun provideSetupPreferences(
        @ApplicationContext context: Context,
    ): SetupPreferences =
        com.niumi.system.setup
            .DataStoreSetupPreferences(context)

    @Provides
    fun provideReadinessSources(
        nfcReader: NfcReader,
        pairedBoxStore: PairedBoxStore,
        appSelectionSource: AppSelectionSource,
        alarmScheduler: AlarmScheduler,
        notificationAvailability: NotificationAvailability,
        notificationChannelStatus: NotificationChannelStatus,
        alarmVolumeSource: AlarmVolumeSource,
        interruptionFilterSource: InterruptionFilterSource,
        accessibilityServiceStatus: AccessibilityServiceStatus,
        batteryOptimizationStatus: BatteryOptimizationStatus,
        setupPreferences: SetupPreferences,
    ): ReadinessSources =
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

    @Provides
    fun provideDeviceReadinessChecker(
        sources: ReadinessSources,
        clock: Clock,
    ): DeviceReadinessChecker = AndroidDeviceReadinessChecker(sources, clock, Build.VERSION.SDK_INT)

    /**
     * `@Singleton` obligatoire : l'état de déduplication de §13.1 (« une seule fois tant que
     * l'état ne change pas ») vit dans l'instance.
     */
    @Provides
    @Singleton
    fun provideSessionReadinessMonitor(
        readinessChecker: DeviceReadinessChecker,
        warningNotifier: SessionWarningNotifier,
        eventFactory: SessionEventFactory,
        technicalEventLog: TechnicalEventLog,
    ): SessionReadinessMonitor =
        SessionReadinessMonitor(readinessChecker, warningNotifier, eventFactory, technicalEventLog)

    @Provides
    @Singleton
    fun provideSessionReadinessWatcher(
        publisher: SessionSnapshotPublisher,
        monitor: SessionReadinessMonitor,
        coordinator: SessionCoordinator,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): SessionReadinessWatcher = SessionReadinessWatcher(publisher, monitor, coordinator, dispatcher)
}
