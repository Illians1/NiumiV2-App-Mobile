package com.niumi.system.session

import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.ActivationRequestDto
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.system.common.Clock
import com.niumi.system.common.IdGenerator

/**
 * Construit les événements que le coordinateur s'envoie à lui-même après exécution des effets
 * (SPEC_CORE_KMP §10, §12) : [SessionEventDto] n'a aucune valeur par défaut hors `nfcProof`, cette
 * fabrique évite neuf arguments explicites à chaque site d'appel.
 */
class SessionEventFactory(
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    /**
     * Expose l'horloge injectée aux appelants qui construisent leurs propres incidents
     * ([SessionReconciler]) : leur évite de dépendre directement de [Clock], au prix d'un
     * paramètre de constructeur en moins (`LongParameterList` de detekt).
     */
    fun nowEpochMillis(): Long = clock.nowEpochMillis()

    fun buildIncident(
        code: String,
        severity: IncidentSeverityDto,
    ) = SessionIncidentDto(code, severity, clock.nowEpochMillis(), PlatformDto.ANDROID)

    /**
     * Seul événement que `ArmSessionUseCase` dispatche lui-même (étape 14) : aucun snapshot
     * préalable n'existe encore, `sessionId` est donc généré ici plutôt que dérivé d'un
     * `snapshot` comme [base] le fait pour toutes les suites. `expectedRevision` reste `null`,
     * la révision n'existant qu'à partir de `PREPARING` (SPEC_CORE_KMP §5.2).
     */
    fun activationRequested(request: ActivationRequestDto) =
        SessionEventDto(
            eventId = idGenerator.newId(),
            sessionId = idGenerator.newId(),
            kind = SessionEventKindDto.ACTIVATION_REQUESTED,
            occurredAtEpochMillis = clock.nowEpochMillis(),
            expectedRevision = null,
            activationRequest = request,
            failureCode = null,
            incident = null,
        )

    fun activationSucceeded(snapshot: SessionSnapshotDto) = base(snapshot, SessionEventKindDto.ACTIVATION_SUCCEEDED)

    fun activationFailed(
        snapshot: SessionSnapshotDto,
        failureCode: String,
    ) = base(snapshot, SessionEventKindDto.ACTIVATION_FAILED, failureCode = failureCode)

    fun releaseSucceeded(snapshot: SessionSnapshotDto) = base(snapshot, SessionEventKindDto.RELEASE_SUCCEEDED)

    fun releaseFailedWithPartialFailure(snapshot: SessionSnapshotDto) =
        base(
            snapshot,
            SessionEventKindDto.RELEASE_FAILED,
            incident = buildIncident(IncidentCodes.RELEASE_PARTIAL_FAILURE, IncidentSeverityDto.DEGRADED),
        )

    fun incidentReported(
        snapshot: SessionSnapshotDto,
        incident: SessionIncidentDto,
    ) = base(snapshot, SessionEventKindDto.INCIDENT_REPORTED, incident = incident)

    fun triggerElapsed(
        snapshot: SessionSnapshotDto,
        incident: SessionIncidentDto?,
    ) = base(snapshot, SessionEventKindDto.TRIGGER_ELAPSED, incident = incident)

    private fun base(
        snapshot: SessionSnapshotDto,
        kind: SessionEventKindDto,
        failureCode: String? = null,
        incident: SessionIncidentDto? = null,
    ) = SessionEventDto(
        eventId = idGenerator.newId(),
        sessionId = snapshot.sessionId,
        kind = kind,
        occurredAtEpochMillis = clock.nowEpochMillis(),
        expectedRevision = snapshot.revision,
        activationRequest = null,
        failureCode = failureCode,
        incident = incident,
    )
}
