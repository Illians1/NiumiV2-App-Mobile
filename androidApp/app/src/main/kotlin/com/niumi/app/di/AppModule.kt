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
    // dépendre (SPEC_ANDROID §6) : `:app` résout, `:core:system` consomme l'identifiant. Le rond
    // de l'icône dans le volet prend l'encre du fond de l'icône du lanceur, pour montrer la même
    // image (décision de Mehdi du 2026-10-31 ; sans couleur, le système en tirait une du fond d'écran).
    @Provides
    @Singleton
    fun provideNotificationIconResolver(
        @ApplicationContext context: Context,
    ): NotificationIconResolver =
        object : NotificationIconResolver {
            override fun smallIconResId(): Int = R.drawable.ic_niumi_notification

            override fun accentColor(): Int = context.getColor(com.niumi.app.R.color.ic_launcher_background)
        }
}
