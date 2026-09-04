package com.niumi.database.logging.di

import com.niumi.database.logging.InMemoryTechnicalEventLog
import com.niumi.database.logging.TechnicalEventLog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
    @Provides
    @Singleton
    fun provideTechnicalEventLog(): TechnicalEventLog = InMemoryTechnicalEventLog()
}
