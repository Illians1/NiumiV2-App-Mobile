package com.niumi.system.session.di

import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionRuntimeReconciler
import com.niumi.system.session.SessionRuntimeStatusProbe
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binding de [SessionRuntimeReconciler] (SPEC_ANDROID §7.1, §18 ; étape 20). Module dédié plutôt
 * qu'ajouté à [SessionModule], déjà à son plafond `TooManyFunctions` (11) — voir son KDoc.
 */
@Module
@InstallIn(SingletonComponent::class)
object RuntimeModule {
    @Provides
    fun provideSessionRuntimeReconciler(
        probe: SessionRuntimeStatusProbe,
        alarmScheduler: AlarmScheduler,
        eventFactory: SessionEventFactory,
        incidentsReader: SessionIncidentsReader,
        technicalEventLog: TechnicalEventLog,
    ): SessionRuntimeReconciler =
        SessionRuntimeReconciler(probe, alarmScheduler, eventFactory, incidentsReader, technicalEventLog)
}
