package com.niumi.system.notification.di

import android.content.Context
import com.niumi.system.common.DeviceProtected
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

/**
 * Bindings de la notification d'attente de scan (SPEC_ANDROID §10.5) et de ses sondes (§7.1).
 * [provideScanRequestNotifier] utilise le contexte protégé par appareil ([DeviceProtected]) : la
 * notification doit pouvoir être publiée et retirée avant le premier déverrouillage, depuis le
 * coordinateur Direct Boot (SPEC_ANDROID §9.3, §10.5).
 */
@Module
@InstallIn(SingletonComponent::class)
object ScanRequestModule {
    @Provides
    @Singleton
    fun provideScanRequestNotifier(
        @DeviceProtected context: Context,
        pendingIntentFactory: AndroidPendingIntentFactory,
        iconResolver: NotificationIconResolver,
    ): ScanRequestNotifier = AndroidScanRequestNotifier(context, pendingIntentFactory, iconResolver)

    @Provides
    @Singleton
    fun provideNotificationAvailability(
        @ApplicationContext context: Context,
    ): NotificationAvailability = AndroidNotificationAvailability(context)
}
