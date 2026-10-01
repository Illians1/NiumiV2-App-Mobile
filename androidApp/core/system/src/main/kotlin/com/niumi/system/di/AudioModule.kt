package com.niumi.system.di

import android.content.Context
import com.niumi.system.audio.AlarmAudioEngine
import com.niumi.system.audio.AlarmPlayerFactory
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.audio.AndroidAlarmVolumeSource
import com.niumi.system.audio.AndroidAudioFocusController
import com.niumi.system.audio.AndroidVibrationController
import com.niumi.system.audio.AudioFocusController
import com.niumi.system.audio.DefaultAlarmAudioEngine
import com.niumi.system.audio.DefaultRingtonePreviewPlayer
import com.niumi.system.audio.MediaPlayerAlarmPlayerFactory
import com.niumi.system.audio.PreviewPlayerFactory
import com.niumi.system.audio.RingtonePreviewPlayer
import com.niumi.system.audio.RingtoneResourceResolver
import com.niumi.system.audio.VibrationController
import com.niumi.system.common.Clock
import com.niumi.system.common.DefaultDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    fun provideMediaPlayerFactory(
        @ApplicationContext context: Context,
        ringtoneResolver: RingtoneResourceResolver,
    ): MediaPlayerAlarmPlayerFactory = MediaPlayerAlarmPlayerFactory(context, ringtoneResolver)

    @Provides
    fun provideAlarmPlayerFactory(factory: MediaPlayerAlarmPlayerFactory): AlarmPlayerFactory = factory

    @Provides
    fun providePreviewPlayerFactory(factory: MediaPlayerAlarmPlayerFactory): PreviewPlayerFactory = factory

    /**
     * La rampe du Lot 7 vit dans un scope propre au moteur, singleton comme lui : elle doit survivre
     * à l'appelant de `start()` (une coroutine du service) et n'être annulée que par `stop()`.
     * `SupervisorJob` : l'échec d'une rampe n'annule pas les suivantes.
     */
    @Provides
    @Singleton
    fun provideAlarmAudioEngine(
        playerFactory: AlarmPlayerFactory,
        focusController: AudioFocusController,
        vibrationController: VibrationController,
        clock: Clock,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): AlarmAudioEngine =
        DefaultAlarmAudioEngine(
            playerFactory,
            focusController,
            vibrationController,
            clock,
            CoroutineScope(SupervisorJob() + dispatcher),
        )

    /** Pré-écoute de l'écran 14 (Lot 7) : même fabrique `USAGE_ALARM`, jamais le moteur de réveil. */
    @Provides
    @Singleton
    fun provideRingtonePreviewPlayer(
        playerFactory: PreviewPlayerFactory,
        focusController: AudioFocusController,
    ): RingtonePreviewPlayer = DefaultRingtonePreviewPlayer(playerFactory, focusController)

    @Provides
    @Singleton
    fun provideAlarmVolumeSource(
        @ApplicationContext context: Context,
    ): AlarmVolumeSource = AndroidAlarmVolumeSource(context)
}
