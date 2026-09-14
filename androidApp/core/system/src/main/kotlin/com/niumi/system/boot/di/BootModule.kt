package com.niumi.system.boot.di

import com.niumi.database.SessionStore
import com.niumi.database.directboot.DirectBootRoomMerge
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.system.boot.DirectBootMerger
import com.niumi.system.boot.SystemEventsRegistrar
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.session.SessionCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Bindings des événements système (SPEC_ANDROID §9.3, étape 19). Module dédié plutôt qu'ajouté à
 * `SessionModule`, déjà au plafond `TooManyFunctions` de detekt (11) — même motif que
 * `TriggerModule` et `EffectExecutorModule`.
 */
@Module
@InstallIn(SingletonComponent::class)
object BootModule {
    @Provides
    @Singleton
    fun provideDirectBootMerger(
        unlockState: UnlockState,
        directBootStore: DirectBootStore,
        sessionStore: SessionStore,
        roomMerge: DirectBootRoomMerge,
    ): DirectBootMerger = DirectBootMerger(unlockState, directBootStore, sessionStore, roomMerge)

    @Provides
    @Singleton
    fun provideSystemEventsRegistrar(
        coordinator: SessionCoordinator,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): SystemEventsRegistrar = SystemEventsRegistrar(coordinator, dispatcher)
}
