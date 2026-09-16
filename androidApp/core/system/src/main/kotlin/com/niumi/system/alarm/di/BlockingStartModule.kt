package com.niumi.system.alarm.di

import android.content.Context
import com.niumi.system.alarm.AndroidBlockingStartScheduler
import com.niumi.system.alarm.BlockingStartScheduler
import com.niumi.system.common.DeviceProtected
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.intent.NiumiComponentResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Alarme de début du blocage différé (SPEC_ANDROID §12.4 ; Lot 6). Module séparé de `SystemModule`,
 * déjà au plafond detekt `TooManyFunctions` — même motif que `TriggerModule` et `RuntimeModule`.
 *
 * `@DeviceProtected` comme les autres adaptateurs d'alarme : le contexte protégé par appareil est
 * employé **en permanence** et non aiguillé selon l'état de déverrouillage, `AlarmManager` se
 * comportant à l'identique avec l'un ou l'autre (SPEC_ANDROID §7.3, précision de l'étape 19).
 */
@Module
@InstallIn(SingletonComponent::class)
object BlockingStartModule {
    @Provides
    @Singleton
    fun provideBlockingStartScheduler(
        @DeviceProtected context: Context,
        resolver: NiumiComponentResolver,
        pendingIntentFactory: AndroidPendingIntentFactory,
    ): BlockingStartScheduler = AndroidBlockingStartScheduler(context, resolver, pendingIntentFactory)
}
