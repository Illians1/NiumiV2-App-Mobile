package com.niumi.system.di

import android.content.Context
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import com.niumi.system.notification.NotificationIconResolver
import com.niumi.system.notification.RingingNotificationFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Bindings notification (SPEC_ANDROID §10.3, §10.5). */
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
}
