package com.niumi.system.notification.di

import android.content.Context
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.notification.AndroidNotificationAvailability
import com.niumi.system.notification.AndroidScanRequestNotifier
import com.niumi.system.notification.NotificationAvailability
import com.niumi.system.notification.NotificationIconResolver
import com.niumi.system.notification.ScanRequestNotifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Bindings de la notification d'attente de scan (SPEC_ANDROID §10.5) et de ses sondes (§7.1). */
@Module
@InstallIn(SingletonComponent::class)
object ScanRequestModule {
    @Provides
    @Singleton
    fun provideScanRequestNotifier(
        @ApplicationContext context: Context,
        pendingIntentFactory: AndroidPendingIntentFactory,
        iconResolver: NotificationIconResolver,
    ): ScanRequestNotifier = AndroidScanRequestNotifier(context, pendingIntentFactory, iconResolver)

    @Provides
    @Singleton
    fun provideNotificationAvailability(
        @ApplicationContext context: Context,
    ): NotificationAvailability = AndroidNotificationAvailability(context)
}
