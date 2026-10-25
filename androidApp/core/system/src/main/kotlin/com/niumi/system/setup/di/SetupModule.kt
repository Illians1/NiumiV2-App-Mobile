package com.niumi.system.setup.di

import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionSource
import com.niumi.system.setup.SetupCompletion
import com.niumi.system.setup.SetupPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Étape 45 : l'accueil s'ouvre directement quand la mise en route est finie. */
@Module
@InstallIn(SingletonComponent::class)
object SetupModule {
    @Provides
    fun provideSetupCompletion(
        preferences: SetupPreferences,
        pairedBoxStore: PairedBoxStore,
        appSelectionSource: AppSelectionSource,
    ): SetupCompletion = SetupCompletion(preferences, pairedBoxStore, appSelectionSource)
}
