package com.niumi.feature.session.diagnostics

import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import javax.inject.Inject

/**
 * Les cinq lectures dont l'écran 12 a besoin (SPEC_ANDROID §15, §17), groupées : elles sont toutes
 * des **sources**, et les énumérer à plat faisait dépasser `LongParameterList` de detekt sur le
 * constructeur du ViewModel. Même motif que `ReconcilerSources` et `ReadinessSources`
 * (`:core:system`).
 *
 * Aucune n'écrit : le diagnostic constate, il ne décide de rien.
 */
class DiagnosticSources
    @Inject
    constructor(
        val snapshotPublisher: SessionSnapshotPublisher,
        val gateway: SessionPersistenceGateway,
        val incidentsReader: SessionIncidentsReader,
        val technicalEventLog: TechnicalEventLog,
        val readinessChecker: DeviceReadinessChecker,
    )
