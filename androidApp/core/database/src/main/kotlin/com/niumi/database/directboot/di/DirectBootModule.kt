package com.niumi.database.directboot.di

import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.FileDirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.database.directboot.UserManagerUnlockState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `@Binds` plutôt que `@Provides` : les deux implémentations n'ont besoin que de leur propre
 * constructeur `@Inject`, aucune construction manuelle à écrire ici.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DirectBootModule {
    @Binds
    @Singleton
    fun bindUnlockState(impl: UserManagerUnlockState): UnlockState

    @Binds
    @Singleton
    fun bindDirectBootStore(impl: FileDirectBootStore): DirectBootStore
}
