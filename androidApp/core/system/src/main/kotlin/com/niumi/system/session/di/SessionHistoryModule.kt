package com.niumi.system.session.di

import com.niumi.database.SessionHistoryStore
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.system.session.SessionHistoryReader
import com.niumi.system.session.UnlockAwareSessionHistoryReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Session suivie par identifiant (étape 46, écran 11 et feuille d'annulation). Module à part :
 * [SessionModule] est au plafond detekt `TooManyFunctions`.
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionHistoryModule {
    @Provides
    @Singleton
    fun provideSessionHistoryReader(
        historyStore: SessionHistoryStore,
        directBootStore: DirectBootStore,
        unlockState: UnlockState,
    ): SessionHistoryReader = UnlockAwareSessionHistoryReader(historyStore, directBootStore, unlockState)
}
