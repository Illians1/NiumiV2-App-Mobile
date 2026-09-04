package com.niumi.feature.ringing.di

import android.content.Context
import com.niumi.feature.ringing.AndroidRingingController
import com.niumi.feature.ringing.R
import com.niumi.system.audio.RingtoneResourceResolver
import com.niumi.system.ringing.RingingController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `:core:system` ne peut pas référencer le `R` de `:feature:ringing`, propriétaire du fichier
 * audio empaqueté ([RingtoneResourceResolver]) ni des classes cibles des `PendingIntent`
 * ([RingingController]) : ces bindings vivent ici, module downstream (SPEC_ANDROID §6).
 */
@Module
@InstallIn(SingletonComponent::class)
object RingingModule {
    @Provides
    @Singleton
    fun provideRingtoneResourceResolver(): RingtoneResourceResolver =
        RingtoneResourceResolver { ringtoneKey ->
            if (ringtoneKey == "niumi_alarm") R.raw.niumi_alarm else null
        }

    @Provides
    @Singleton
    fun provideRingingController(
        @ApplicationContext context: Context,
    ): RingingController = AndroidRingingController(context)
}
