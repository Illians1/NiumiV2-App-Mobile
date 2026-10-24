package com.niumi.system.plan.di

import android.content.Context
import com.niumi.database.directboot.UnlockState
import com.niumi.system.insight.DataStoreTimeToGetUpPreferences
import com.niumi.system.insight.TimeToGetUpPreferences
import com.niumi.system.plan.DataStoreWakePlanStore
import com.niumi.system.plan.WakePlanStore
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
}
