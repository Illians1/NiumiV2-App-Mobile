package com.niumi.system.session.di

import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.blocking.BlockingController
import com.niumi.system.common.Clock
import com.niumi.system.notification.ScanRequestNotifier
import com.niumi.system.ringing.RingingController
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.executors.ApplyBlockingExecutor
import com.niumi.system.session.executors.CancelAlarmExecutor
import com.niumi.system.session.executors.ClearActiveSessionExecutor
import com.niumi.system.session.executors.ClearScanRequestExecutor
import com.niumi.system.session.executors.PresentScanRequestExecutor
import com.niumi.system.session.executors.PublishSnapshotExecutor
import com.niumi.system.session.executors.RecordIncidentExecutor
import com.niumi.system.session.executors.RemoveBlockingExecutor
import com.niumi.system.session.executors.ScheduleAlarmExecutor
import com.niumi.system.session.executors.StartRingingExecutor
import com.niumi.system.session.executors.StopRingingExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Table `SessionEffectKind → EffectExecutor` (SPEC_CORE_KMP §6), les onze exécuteurs de l'étape
 * 11. Une seule fonction plutôt qu'un `@Provides` par exécuteur : les construire directement dans
 * la table évite d'exposer chaque exécuteur comme un type injectable à part entière, ce que rien
 * d'autre ne consomme.
 */
@Module
@InstallIn(SingletonComponent::class)
object EffectExecutorModule {
    @Provides
    fun provideEffectExecutors(
        publisher: SessionSnapshotPublisher,
        technicalEventLog: TechnicalEventLog,
        alarmScheduler: AlarmScheduler,
        blockingController: BlockingController,
        clock: Clock,
        ringingController: RingingController,
        scanRequestNotifier: ScanRequestNotifier,
        gateway: SessionPersistenceGateway,
    ): Map<SessionEffectKindDto, EffectExecutor> =
        mapOf(
            SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT to PublishSnapshotExecutor(publisher, technicalEventLog),
            SessionEffectKindDto.SCHEDULE_ALARM to ScheduleAlarmExecutor(alarmScheduler, technicalEventLog),
            SessionEffectKindDto.CANCEL_ALARM to CancelAlarmExecutor(alarmScheduler),
            SessionEffectKindDto.APPLY_BLOCKING to ApplyBlockingExecutor(blockingController, technicalEventLog),
            SessionEffectKindDto.REMOVE_BLOCKING to RemoveBlockingExecutor(blockingController, clock),
            SessionEffectKindDto.START_RINGING to StartRingingExecutor(ringingController, technicalEventLog),
            SessionEffectKindDto.STOP_RINGING to StopRingingExecutor(ringingController),
            SessionEffectKindDto.PRESENT_SCAN_REQUEST to
                PresentScanRequestExecutor(scanRequestNotifier, technicalEventLog),
            SessionEffectKindDto.CLEAR_SCAN_REQUEST to ClearScanRequestExecutor(scanRequestNotifier, technicalEventLog),
            SessionEffectKindDto.CLEAR_ACTIVE_SESSION to ClearActiveSessionExecutor(gateway),
            SessionEffectKindDto.RECORD_INCIDENT to RecordIncidentExecutor(gateway),
        )
}
