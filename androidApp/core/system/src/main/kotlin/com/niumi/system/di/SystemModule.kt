package com.niumi.system.di

import android.content.Context
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.alarm.AndroidAlarmScheduler
import com.niumi.system.common.Clock
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.common.IdGenerator
import com.niumi.system.common.SystemClock
import com.niumi.system.common.UuidIdGenerator
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.intent.NiumiComponentResolver
import com.niumi.system.power.AndroidWakeLockHolder
import com.niumi.system.power.WakeLockHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Bindings communs et alarme (« Interfaces transverses » du plan MVP). Les résolveurs
 * dépendant d'un module downstream ([NiumiComponentResolver]) sont injectés en paramètre :
 * leurs propres bindings vivent dans `:app`. Les bindings audio et notification sont dans
 * [AudioModule] et [SystemNotificationModule] : au-delà de 11 fonctions, `TooManyFunctions`
 * de detekt exige de scinder par responsabilité plutôt que d'assouplir la règle.
 */
@Module
@InstallIn(SingletonComponent::class)
object SystemModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = UuidIdGenerator()

    @Provides
    @DefaultDispatcher
    @Suppress("InjectDispatcher") // Seul endroit légitime : c'est le point d'injection lui-même.
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun providePendingIntentFactory(
        @ApplicationContext context: Context,
        resolver: NiumiComponentResolver,
    ): AndroidPendingIntentFactory = AndroidPendingIntentFactory(context, resolver)

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        resolver: NiumiComponentResolver,
        pendingIntentFactory: AndroidPendingIntentFactory,
    ): AlarmScheduler = AndroidAlarmScheduler(context, resolver, pendingIntentFactory)

    @Provides
    fun provideWakeLockHolder(
        @ApplicationContext context: Context,
    ): WakeLockHolder = AndroidWakeLockHolder(context)
}
