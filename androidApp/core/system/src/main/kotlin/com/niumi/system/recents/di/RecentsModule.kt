package com.niumi.system.recents.di

import android.content.Context
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.NiumiComponentResolver
import com.niumi.system.recents.AndroidRecentsCard
import com.niumi.system.recents.AndroidRecentsLockStatus
import com.niumi.system.recents.RecentsCard
import com.niumi.system.recents.RecentsLockStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * Liaison de [RecentsCard] (étape 25). Module dédié plutôt qu'ajouté à `SystemModule`, déjà à son
 * plafond `TooManyFunctions` (11). L'activité principale vient de [NiumiComponentResolver] :
 * `:core:system` ne peut pas référencer `:app` (SPEC_ANDROID §6).
 */
@Module
@InstallIn(SingletonComponent::class)
object RecentsModule {
    @Provides
    fun provideRecentsCard(
        @ApplicationContext context: Context,
        resolver: NiumiComponentResolver,
    ): RecentsCard = AndroidRecentsCard(context, resolver.componentName(NiumiComponent.MAIN_ACTIVITY))

    @Provides
    fun provideRecentsLockStatus(
        @ApplicationContext context: Context,
    ): RecentsLockStatus = AndroidRecentsLockStatus(context)
}
