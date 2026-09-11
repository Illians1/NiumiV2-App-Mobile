package com.niumi.database.di

import com.niumi.database.NiumiDatabase
import com.niumi.database.directboot.UnlockState
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.database.pairing.RoomPairedBoxStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * `@Provides` plutôt que `@Binds` : le constructeur de [RoomPairedBoxStore] porte un paramètre par
 * défaut (`nowEpochMillis`) que Dagger ne respecte pas (même motif que [SessionStoreModule]).
 */
@Module
@InstallIn(SingletonComponent::class)
object PairedBoxStoreModule {
    @Provides
    @Singleton
    fun providePairedBoxStore(
        databaseProvider: Provider<NiumiDatabase>,
        unlockState: UnlockState,
    ): PairedBoxStore = RoomPairedBoxStore(databaseProvider, unlockState)
}
