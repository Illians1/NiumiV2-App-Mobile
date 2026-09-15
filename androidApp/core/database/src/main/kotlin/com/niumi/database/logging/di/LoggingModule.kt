package com.niumi.database.logging.di

import android.content.Context
import com.niumi.database.dao.TechnicalEventDao
import com.niumi.database.logging.DeviceContext
import com.niumi.database.logging.InMemoryTechnicalEventLog
import com.niumi.database.logging.RoomTechnicalEventLog
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventLogFlush
import com.niumi.database.logging.UnlockAwareTechnicalEventLog
import com.niumi.database.logging.readDeviceContext
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /**
     * Même implémentation que [bindTechnicalEventLog], donc la même instance `@Singleton` : un
     * second graphe donnerait un [com.niumi.database.logging.InMemoryTechnicalEventLog] jamais
     * alimenté par les seize sites d'appel de `log()`, et [TechnicalEventLogFlush.flush] viderait
     * un journal vide.
     */
    @Binds
    @Singleton
    fun bindTechnicalEventLogFlush(impl: UnlockAwareTechnicalEventLog): TechnicalEventLogFlush

    companion object {
        /**
         * Contexte d'appareil de SPEC_ANDROID §17. Lu une fois par processus : `Build.MODEL` et
         * `Build.VERSION` sont des constantes du système, et la version de l'application ne change
         * pas sans redémarrage du processus (`PACKAGE_REPLACED` le tue).
         */
        @Provides
        @Singleton
        fun provideDeviceContext(
            @ApplicationContext context: Context,
        ): DeviceContext = readDeviceContext(context)

        @Provides
        @Singleton
        fun provideInMemoryTechnicalEventLog(deviceContext: DeviceContext): InMemoryTechnicalEventLog =
            InMemoryTechnicalEventLog(deviceContext)

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
            deviceContext: DeviceContext,
        ): RoomTechnicalEventLog = RoomTechnicalEventLog(dao, scope, deviceContext)
    }
}
