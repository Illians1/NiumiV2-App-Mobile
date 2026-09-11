package com.niumi.database.di

import com.niumi.database.NiumiDatabase
import com.niumi.database.RoomSessionStore
import com.niumi.database.SessionStore
import com.niumi.database.directboot.UnlockState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Aucun consommateur de production à cette étape (`SessionCoordinator` arrive à l'étape 11) :
 * binding fourni pour fixer le contrat, résolu paresseusement par Hilt tant que rien ne l'injecte.
 * `Provider<NiumiDatabase>` plutôt que `NiumiDatabase` : la garde `ROOM_BEFORE_UNLOCK` de
 * `RoomSessionStore` (étape 10) doit pouvoir refuser un accès sans jamais faire résoudre la base
 * par Hilt (SPEC_ANDROID §7.3).
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionStoreModule {
    @Provides
    @Singleton
    fun provideSessionStore(
        databaseProvider: Provider<NiumiDatabase>,
        unlockState: UnlockState,
    ): SessionStore = RoomSessionStore(databaseProvider, unlockState)
}
