package com.niumi.app.di

import android.content.Context
import com.niumi.app.system.AppComponentResolver
import com.niumi.designsystem.R
import com.niumi.system.intent.NiumiComponentResolver
import com.niumi.system.notification.NotificationIconResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideNiumiComponentResolver(
        @ApplicationContext context: Context,
    ): NiumiComponentResolver = AppComponentResolver(context)

    // L'asset vit dans :core:designsystem, dont seuls `:app` et les modules `feature` peuvent
    // dépendre (SPEC_ANDROID §6) : `:app` résout, `:core:system` consomme l'identifiant.
    @Provides
    @Singleton
    fun provideNotificationIconResolver(): NotificationIconResolver =
        NotificationIconResolver { R.drawable.ic_niumi_notification }
}
