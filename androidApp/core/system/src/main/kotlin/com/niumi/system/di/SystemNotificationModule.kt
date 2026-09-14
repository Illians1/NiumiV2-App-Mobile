package com.niumi.system.di

import android.content.Context
import com.niumi.system.common.DeviceProtected
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import com.niumi.system.notification.AndroidSessionWarningNotifier
import com.niumi.system.notification.NotificationIconResolver
import com.niumi.system.notification.RingingNotificationFactory
import com.niumi.system.notification.SessionWarningNotifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings notification (SPEC_ANDROID §10.3, §10.5, §13.1).
 *
 * Deux d'entre eux sont sur le contexte protégé par appareil ([DeviceProtected]) : le canal
 * `niumi_session_awaiting_scan` doit exister avant le premier déverrouillage — poster sur un canal
 * inconnu fait rejeter la notification — et la surveillance de §13.1 est rejouée par la
 * réconciliation `LOCKED_BOOT`, donc son avertissement aussi. [provideRingingNotificationFactory]
 * reste sur `@ApplicationContext` : elle n'est construite que par `AlarmRingingService`, qui porte
 * déjà son propre `directBootAware`.
 */
@Module
@InstallIn(SingletonComponent::class)
object SystemNotificationModule {
    @Provides
    @Singleton
    fun provideNotificationChannelRegistrar(
        @DeviceProtected context: Context,
    ): AndroidNotificationChannelRegistrar = AndroidNotificationChannelRegistrar(context)

    @Provides
    @Singleton
    fun provideRingingNotificationFactory(
        @ApplicationContext context: Context,
        iconResolver: NotificationIconResolver,
    ): RingingNotificationFactory = RingingNotificationFactory(context, iconResolver)

    @Provides
    @Singleton
    fun provideSessionWarningNotifier(
        @DeviceProtected context: Context,
        pendingIntentFactory: AndroidPendingIntentFactory,
        iconResolver: NotificationIconResolver,
    ): SessionWarningNotifier = AndroidSessionWarningNotifier(context, pendingIntentFactory, iconResolver)
}
