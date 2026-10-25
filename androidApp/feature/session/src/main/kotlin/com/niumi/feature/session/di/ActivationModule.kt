package com.niumi.feature.session.di

import com.niumi.database.pairing.PairedBoxStore
import com.niumi.feature.session.activation.ActivationSources
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.common.TimeZoneProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * [ActivationSources] n'a pas de constructeur `@Inject` : c'est un regroupement de dépôts déjà
 * liés ailleurs, assemblé ici comme `ReadinessSources` l'est dans `:core:system`.
 * `ArmSessionUseCase`, lui, porte son `@Inject constructor` et n'a besoin d'aucun binding.
 */
@Module
@InstallIn(SingletonComponent::class)
object ActivationModule {
    @Provides
    @Singleton
    fun provideActivationSources(
        pairedBoxStore: PairedBoxStore,
        appSelectionStore: AppSelectionStore,
        timeZoneProvider: TimeZoneProvider,
    ): ActivationSources = ActivationSources(pairedBoxStore, appSelectionStore, timeZoneProvider)
}
