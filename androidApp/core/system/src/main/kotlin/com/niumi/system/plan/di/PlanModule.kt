package com.niumi.system.plan.di

import android.content.Context
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.database.directboot.UnlockState
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.insight.DataStoreTimeToGetUpPreferences
import com.niumi.system.insight.TimeToGetUpPreferences
import com.niumi.system.plan.ArmOccurrenceUseCase
import com.niumi.system.plan.DataStoreWakePlanStore
import com.niumi.system.plan.DefaultWakePlanScheduler
import com.niumi.system.plan.PlanArmingStatus
import com.niumi.system.plan.WakePlanScheduler
import com.niumi.system.plan.WakePlanSchedulerSources
import com.niumi.system.plan.WakePlanStore
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionPassHook
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Planning et durée déclarée (étape 43) : binding séparé de `ReadinessModule`, déjà au plafond
 * `TooManyFunctions` de detekt (`ETAPE-24.md`). `Provider<Context>` : jamais résolu avant
 * déverrouillage (SPEC_ANDROID §7.3).
 *
 * Étape 44 : le `WakePlanScheduler` et son crochet de passe sont **la même instance**
 * ([DefaultWakePlanScheduler], singleton) ; `SessionModule` la reçoit par `Provider<SessionPassHook>`.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlanModule {
    @Provides
    @Singleton
    fun provideWakePlanStore(
        @ApplicationContext contextProvider: Provider<Context>,
        unlockState: UnlockState,
    ): WakePlanStore = DataStoreWakePlanStore(contextProvider, unlockState)

    @Provides
    @Singleton
    fun provideTimeToGetUpPreferences(
        @ApplicationContext contextProvider: Provider<Context>,
        unlockState: UnlockState,
    ): TimeToGetUpPreferences = DataStoreTimeToGetUpPreferences(contextProvider, unlockState)

    @Provides
    @Singleton
    fun providePlanArmingStatus(): PlanArmingStatus = PlanArmingStatus()

    @Provides
    fun provideArmOccurrenceUseCase(
        facade: NiumiCoreFacade,
        eventFactory: SessionEventFactory,
        pairedBoxStore: PairedBoxStore,
        appSelectionStore: AppSelectionStore,
        alarmSoundPreferences: AlarmSoundPreferences,
    ): ArmOccurrenceUseCase =
        ArmOccurrenceUseCase(facade, eventFactory, pairedBoxStore, appSelectionStore, alarmSoundPreferences)

    @Provides
    @Singleton
    fun provideDefaultWakePlanScheduler(
        coordinator: SessionCoordinator,
        armOccurrence: ArmOccurrenceUseCase,
        facade: NiumiCoreFacade,
        eventFactory: SessionEventFactory,
        planStore: WakePlanStore,
        unlockState: UnlockState,
        timeZoneProvider: TimeZoneProvider,
        technicalEventLog: TechnicalEventLog,
        armingStatus: PlanArmingStatus,
    ): DefaultWakePlanScheduler =
        DefaultWakePlanScheduler(
            coordinator,
            armOccurrence,
            facade,
            eventFactory,
            WakePlanSchedulerSources(planStore, unlockState, timeZoneProvider, technicalEventLog, armingStatus),
        )

    @Provides
    fun provideWakePlanScheduler(scheduler: DefaultWakePlanScheduler): WakePlanScheduler = scheduler

    @Provides
    fun provideSessionPassHook(scheduler: DefaultWakePlanScheduler): SessionPassHook = scheduler
}
