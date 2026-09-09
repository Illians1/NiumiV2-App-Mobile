package com.niumi.database.di

import com.niumi.database.NiumiDatabase
import com.niumi.database.RoomSessionStore
import com.niumi.database.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Aucun consommateur de production à cette étape (`SessionCoordinator` arrive à l'étape 11) :
 * binding fourni pour fixer le contrat, résolu paresseusement par Hilt tant que rien ne l'injecte.
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionStoreModule {
    @Provides
    @Singleton
    fun provideSessionStore(database: NiumiDatabase): SessionStore = RoomSessionStore(database)
}
