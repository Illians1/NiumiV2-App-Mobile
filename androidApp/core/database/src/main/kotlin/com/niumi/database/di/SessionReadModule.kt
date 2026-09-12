package com.niumi.database.di

import com.niumi.database.blocking.BlockedPackagesSource
import com.niumi.database.blocking.RoomBlockedPackagesSource
import com.niumi.database.incident.RoomSessionIncidentsReader
import com.niumi.database.incident.SessionIncidentsReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Liaisons des lectures ajoutées à l'étape 15 : la projection de blocage persistée
 * (SPEC_ANDROID §12.2) et les incidents d'une session (SPEC_CORE_KMP §7.3). Module dédié plutôt
 * qu'ajouté à [DaoModule], déjà au plafond `TooManyFunctions` de detekt (11) — même motif que
 * [SessionStoreModule] et [PairedBoxStoreModule].
 */
@Module
@InstallIn(SingletonComponent::class)
interface SessionReadModule {
    @Binds
    fun bindBlockedPackagesSource(impl: RoomBlockedPackagesSource): BlockedPackagesSource

    @Binds
    fun bindSessionIncidentsReader(impl: RoomSessionIncidentsReader): SessionIncidentsReader
}
