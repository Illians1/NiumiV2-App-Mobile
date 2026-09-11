package com.niumi.system.di

import android.content.Context
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

/** Bindings notification (SPEC_ANDROID §10.3, §10.5, §13.1). */
@Module
@InstallIn(SingletonComponent::class)
object SystemNotificationModule {
    @Provides
    @Singleton
    fun provideNotificationChannelRegistrar(
        @ApplicationContext context: Context,
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
        @ApplicationContext context: Context,
        pendingIntentFactory: AndroidPendingIntentFactory,
        iconResolver: NotificationIconResolver,
    ): SessionWarningNotifier = AndroidSessionWarningNotifier(context, pendingIntentFactory, iconResolver)
}
