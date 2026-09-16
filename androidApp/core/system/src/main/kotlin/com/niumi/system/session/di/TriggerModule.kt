package com.niumi.system.session.di

import com.niumi.database.logging.TechnicalEventLog
import com.niumi.system.readiness.SessionReadinessMonitor
import com.niumi.system.session.AlarmTriggerHandler
import com.niumi.system.session.BlockingStartHandler
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionPersistenceGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Bindings du déclenchement : l'alarme du réveil (étape 17) et le début du blocage différé
 * (Lot 6). Module distinct plutôt qu'une douzième fonction dans [SessionModule], qui est déjà au
 * plafond de `TooManyFunctions` (11) — même motif que [EffectExecutorModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object TriggerModule {
    @Provides
    fun provideAlarmTriggerHandler(
        gateway: SessionPersistenceGateway,
        coordinator: SessionCoordinator,
        eventFactory: SessionEventFactory,
        readinessMonitor: SessionReadinessMonitor,
        technicalEventLog: TechnicalEventLog,
    ): AlarmTriggerHandler =
        AlarmTriggerHandler(gateway, coordinator, eventFactory, readinessMonitor, technicalEventLog)

    /**
     * Pas de `SessionReadinessMonitor` ici, contrairement au déclenchement du réveil : le début du
     * blocage laisse la session `ARMED`, état où la surveillance de §13.1 continue de s'exécuter à
     * chaque réconciliation (SPEC_ANDROID §12.4, « ce que le début différé ne change pas »).
     */
    @Provides
    fun provideBlockingStartHandler(
        gateway: SessionPersistenceGateway,
        coordinator: SessionCoordinator,
        eventFactory: SessionEventFactory,
        technicalEventLog: TechnicalEventLog,
    ): BlockingStartHandler = BlockingStartHandler(gateway, coordinator, eventFactory, technicalEventLog)
}
