package com.niumi.feature.session.diagnostics

import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.StorageIntegrityState
import javax.inject.Inject

/**
 * Les six lectures dont l'écran 12 a besoin (SPEC_ANDROID §15, §17, §18), groupées : elles sont
 * toutes des **sources**, et les énumérer à plat faisait dépasser `LongParameterList` de detekt sur
 * le constructeur du ViewModel. Même motif que `ReconcilerSources` et `ReadinessSources`
 * (`:core:system`).
 *
 * [storageIntegrity] rejoint le groupe à l'étape 20 : quand Room ne se lit plus, l'écran doit
 * l'afficher plutôt que rester en chargement indéfini.
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
        val storageIntegrity: StorageIntegrityState,
    )
