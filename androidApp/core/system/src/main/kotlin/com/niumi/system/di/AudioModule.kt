package com.niumi.system.di

import android.content.Context
import com.niumi.system.audio.AlarmAudioEngine
import com.niumi.system.audio.AlarmPlayerFactory
import com.niumi.system.audio.AndroidAudioFocusController
import com.niumi.system.audio.AndroidVibrationController
import com.niumi.system.audio.AudioFocusController
import com.niumi.system.audio.DefaultAlarmAudioEngine
import com.niumi.system.audio.MediaPlayerAlarmPlayerFactory
import com.niumi.system.audio.RingtoneResourceResolver
import com.niumi.system.audio.VibrationController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings audio (SPEC_ANDROID §10.2). [RingtoneResourceResolver] est injecté en paramètre :
 * son binding vit dans `:feature:ringing`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {
    @Provides
    @Singleton
    fun provideAudioFocusController(
        @ApplicationContext context: Context,
    ): AudioFocusController = AndroidAudioFocusController(context)

    @Provides
    @Singleton
    fun provideVibrationController(
        @ApplicationContext context: Context,
    ): VibrationController = AndroidVibrationController(context)

    @Provides
    @Singleton
    fun provideAlarmPlayerFactory(
        @ApplicationContext context: Context,
        ringtoneResolver: RingtoneResourceResolver,
    ): AlarmPlayerFactory = MediaPlayerAlarmPlayerFactory(context, ringtoneResolver)

    @Provides
    @Singleton
    fun provideAlarmAudioEngine(
        playerFactory: AlarmPlayerFactory,
        focusController: AudioFocusController,
        vibrationController: VibrationController,
    ): AlarmAudioEngine = DefaultAlarmAudioEngine(playerFactory, focusController, vibrationController)
}
