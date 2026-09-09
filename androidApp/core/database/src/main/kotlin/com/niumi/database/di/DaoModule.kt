package com.niumi.database.di

import com.niumi.database.NiumiDatabase
import com.niumi.database.dao.ActiveSessionPointerDao
import com.niumi.database.dao.BlockedAppDao
import com.niumi.database.dao.IncidentDao
import com.niumi.database.dao.OutboxDao
import com.niumi.database.dao.PairedBoxDao
import com.niumi.database.dao.ReceiptDao
import com.niumi.database.dao.SessionDao
import com.niumi.database.dao.TechnicalEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Un provider par DAO (huit tables, SPEC_ANDROID §7.2) : `TooManyFunctions` détekt (11) atteint. */
@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun provideSessionDao(database: NiumiDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideBlockedAppDao(database: NiumiDatabase): BlockedAppDao = database.blockedAppDao()

    @Provides
    fun provideActiveSessionPointerDao(database: NiumiDatabase): ActiveSessionPointerDao =
        database.activeSessionPointerDao()

    @Provides
    fun provideReceiptDao(database: NiumiDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideOutboxDao(database: NiumiDatabase): OutboxDao = database.outboxDao()

    @Provides
    fun provideIncidentDao(database: NiumiDatabase): IncidentDao = database.incidentDao()

    @Provides
    fun providePairedBoxDao(database: NiumiDatabase): PairedBoxDao = database.pairedBoxDao()

    @Provides
    fun provideTechnicalEventDao(database: NiumiDatabase): TechnicalEventDao = database.technicalEventDao()
}
