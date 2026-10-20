package com.niumi.feature.session.blocking.guard.di

import android.content.Context
import com.niumi.feature.session.blocking.guard.AndroidBlockingGuardController
import com.niumi.system.blocking.guard.BlockingGuardController
import com.niumi.system.common.DeviceProtected
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `:core:system` ne peut pas référencer `BlockingGuardService` (SPEC_ANDROID §6) : le contrôleur
 * vit ici, comme `RingingModule` dans `:feature:ringing`. Contexte protégé par appareil : le
 * service peut devoir démarrer avant le premier déverrouillage, après un redémarrage pendant le
 * blocage (§12.5).
 */
@Module
@InstallIn(SingletonComponent::class)
object BlockingGuardModule {
    @Provides
    @Singleton
    fun provideBlockingGuardController(
        @DeviceProtected context: Context,
    ): BlockingGuardController = AndroidBlockingGuardController(context)
}
