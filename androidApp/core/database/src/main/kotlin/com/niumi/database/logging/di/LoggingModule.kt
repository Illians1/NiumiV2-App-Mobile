package com.niumi.database.logging.di

import com.niumi.database.dao.TechnicalEventDao
import com.niumi.database.logging.InMemoryTechnicalEventLog
import com.niumi.database.logging.RoomTechnicalEventLog
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.UnlockAwareTechnicalEventLog
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * `TechnicalEventLog` est désormais [UnlockAwareTechnicalEventLog] (SPEC_ANDROID §7.3,
 * `ETAPE-09.md` écart 9 : dette explicitement reportée à l'étape 10). `InMemoryTechnicalEventLog`
 * et `RoomTechnicalEventLog` gardent leur constructeur sans `@Inject` (paramètre par défaut
 * `nowEpochMillis`, que Dagger ne respecte pas) : ils restent fournis par `@Provides` ici, jamais
 * construits ailleurs.
 */
@Module
@InstallIn(SingletonComponent::class)
interface LoggingModule {
    @Binds
    @Singleton
    fun bindTechnicalEventLog(impl: UnlockAwareTechnicalEventLog): TechnicalEventLog

    companion object {
        @Provides
        @Singleton
        fun provideInMemoryTechnicalEventLog(): InMemoryTechnicalEventLog = InMemoryTechnicalEventLog()

        @Provides
        @Singleton
        @TechnicalEventLogScope
        @Suppress("InjectDispatcher") // Seul endroit légitime : c'est le point d'injection lui-même.
        fun provideTechnicalEventLogScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Provides
        @Singleton
        fun provideRoomTechnicalEventLog(
            dao: TechnicalEventDao,
            @TechnicalEventLogScope scope: CoroutineScope,
        ): RoomTechnicalEventLog = RoomTechnicalEventLog(dao, scope)
    }
}
