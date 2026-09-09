package com.niumi.core.domain

import com.niumi.core.nfc.NfcVerificationProof

// Horodatages de référence, tous distincts et strictement croissants dans l'ordre du parcours
// normal (SPEC_CORE_KMP §5) : activation, armement, déclenchement, arrêt du son, scan, fin.
internal const val CREATED_AT_EPOCH_MILLIS = 1_700_000_000_000L
internal const val ARMED_AT_EPOCH_MILLIS = 1_700_000_001_000L
internal const val TRIGGER_AT_EPOCH_MILLIS = 1_800_000_000_000L
internal const val RINGING_AT_EPOCH_MILLIS = TRIGGER_AT_EPOCH_MILLIS
internal const val ALARM_SOUND_STOPPED_AT_EPOCH_MILLIS = TRIGGER_AT_EPOCH_MILLIS + 1_000L
internal const val TRIGGER_ELAPSED_AT_EPOCH_MILLIS = TRIGGER_AT_EPOCH_MILLIS
internal const val NFC_VERIFIED_AT_EPOCH_MILLIS = TRIGGER_AT_EPOCH_MILLIS + 2_000L
internal const val CANCEL_NFC_VERIFIED_AT_EPOCH_MILLIS = TRIGGER_AT_EPOCH_MILLIS - 1_000L
internal const val RELEASED_AT_EPOCH_MILLIS = TRIGGER_AT_EPOCH_MILLIS + 3_000L

internal const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
internal const val OTHER_SESSION_ID = "22222222-2222-2222-2222-222222222222"
internal const val EVENT_ID_1 = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
internal const val EVENT_ID_2 = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
internal const val BOX_ID = "550e8400-e29b-41d4-a716-446655440000"
internal const val DEFAULT_APP_SELECTION_COUNT = 5

/** `WakeSchedule` de référence commun à toute la suite de tests (plan d'étape 7). */
internal val referenceWakeSchedule =
    WakeSchedule(
        localDateIso = "2026-09-08",
        localTimeIso = "07:00",
        zoneIdAtActivation = "Europe/Paris",
        triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
    )

/**
 * Un snapshot par état de SPEC_CORE_KMP §5. Chaque fonction part de l'état précédent du parcours
 * normal et lui applique `copy()`, ce qui documente au passage l'enchaînement des états.
 */
internal object SessionSnapshotFixtures {
    internal fun preparingSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 1,
        health: SessionHealth = SessionHealth.HEALTHY,
    ) = SessionSnapshot(
        schemaVersion = SessionSnapshot.SCHEMA_VERSION,
        revision = revision,
        sessionId = sessionId,
        wakeSchedule = referenceWakeSchedule,
        state = SessionState.PREPARING,
        releaseTarget = null,
        health = health,
        createdAtEpochMillis = CREATED_AT_EPOCH_MILLIS,
        armedAtEpochMillis = null,
        ringingAtEpochMillis = null,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = null,
        nfcVerifiedAtEpochMillis = null,
        releasingAtEpochMillis = null,
        completedAtEpochMillis = null,
        cancelledAtEpochMillis = null,
        failureCode = null,
    )

    internal fun armedSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 2,
        health: SessionHealth = SessionHealth.HEALTHY,
    ) = preparingSnapshot(sessionId, revision, health).copy(
        state = SessionState.ARMED,
        armedAtEpochMillis = ARMED_AT_EPOCH_MILLIS,
    )

    internal fun ringingSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 3,
        health: SessionHealth = SessionHealth.HEALTHY,
    ) = armedSnapshot(sessionId, revision, health).copy(
        state = SessionState.RINGING,
        ringingAtEpochMillis = RINGING_AT_EPOCH_MILLIS,
    )

    internal fun awaitingNfcSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 4,
        health: SessionHealth = SessionHealth.HEALTHY,
    ) = ringingSnapshot(sessionId, revision, health).copy(
        state = SessionState.AWAITING_NFC,
        alarmSoundStoppedAtEpochMillis = ALARM_SOUND_STOPPED_AT_EPOCH_MILLIS,
    )

    internal fun triggeredAwaitingNfcSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 3,
        health: SessionHealth = SessionHealth.HEALTHY,
    ) = armedSnapshot(sessionId, revision, health).copy(
        state = SessionState.TRIGGERED_AWAITING_NFC,
        triggerElapsedAtEpochMillis = TRIGGER_ELAPSED_AT_EPOCH_MILLIS,
    )

    internal fun releasingSnapshot(
        releaseTarget: ReleaseTarget,
        sessionId: String = SESSION_ID,
        revision: Long = 5,
        health: SessionHealth = SessionHealth.HEALTHY,
    ) = ringingSnapshot(sessionId, revision, health).copy(
        state = SessionState.RELEASING,
        releaseTarget = releaseTarget,
        nfcVerifiedAtEpochMillis = NFC_VERIFIED_AT_EPOCH_MILLIS,
        releasingAtEpochMillis = NFC_VERIFIED_AT_EPOCH_MILLIS,
    )

    internal fun completedSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 6,
        health: SessionHealth = SessionHealth.HEALTHY,
    ) = releasingSnapshot(ReleaseTarget.COMPLETED, sessionId, revision, health).copy(
        state = SessionState.COMPLETED,
        completedAtEpochMillis = RELEASED_AT_EPOCH_MILLIS,
    )

    internal fun cancelledSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 4,
        health: SessionHealth = SessionHealth.HEALTHY,
    ) = armedSnapshot(sessionId, revision, health)
        .copy(
            state = SessionState.RELEASING,
            releaseTarget = ReleaseTarget.CANCELLED,
            nfcVerifiedAtEpochMillis = CANCEL_NFC_VERIFIED_AT_EPOCH_MILLIS,
            releasingAtEpochMillis = CANCEL_NFC_VERIFIED_AT_EPOCH_MILLIS,
        ).copy(
            state = SessionState.CANCELLED,
            cancelledAtEpochMillis = CANCEL_NFC_VERIFIED_AT_EPOCH_MILLIS,
        )

    internal fun failedSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 2,
        failureCode: String = "ANDROID_ALARM_SCHEDULE_FAILED",
    ) = preparingSnapshot(sessionId, revision).copy(
        state = SessionState.FAILED,
        failureCode = failureCode,
    )
}

/** Événements de la famille activation (plan d'étape 7). */
internal object SessionActivationEventFixtures {
    internal fun activationRequest(count: Int = DEFAULT_APP_SELECTION_COUNT) =
        ActivationRequest(wakeSchedule = referenceWakeSchedule, appSelection = AppSelectionSummary(count))

    internal fun activationRequestedEvent(
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_1,
        occurredAtEpochMillis: Long = CREATED_AT_EPOCH_MILLIS,
        activationRequest: ActivationRequest = activationRequest(),
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.ACTIVATION_REQUESTED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = null,
        activationRequest = activationRequest,
        nfcProof = null,
        failureCode = null,
        incident = null,
    )

    internal fun activationSucceededEvent(
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        expectedRevision: Long = 1,
        occurredAtEpochMillis: Long = ARMED_AT_EPOCH_MILLIS,
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.ACTIVATION_SUCCEEDED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = null,
        incident = null,
    )

    internal fun activationFailedEvent(
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        expectedRevision: Long = 1,
        occurredAtEpochMillis: Long = ARMED_AT_EPOCH_MILLIS,
        failureCode: String = "ANDROID_ALARM_SCHEDULE_FAILED",
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.ACTIVATION_FAILED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = failureCode,
        incident = null,
    )
}

/** Événements des familles déclenchement, libération et incident (plan d'étape 7). */
internal object SessionLifecycleEventFixtures {
    internal fun alarmFiredEvent(
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        expectedRevision: Long = 2,
        occurredAtEpochMillis: Long = RINGING_AT_EPOCH_MILLIS,
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.ALARM_FIRED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = null,
        incident = null,
    )

    internal fun alarmSoundStoppedEvent(
        expectedRevision: Long,
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        occurredAtEpochMillis: Long = ALARM_SOUND_STOPPED_AT_EPOCH_MILLIS,
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.ALARM_SOUND_STOPPED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = null,
        incident = null,
    )

    internal fun triggerElapsedEvent(
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        expectedRevision: Long = 2,
        occurredAtEpochMillis: Long = TRIGGER_ELAPSED_AT_EPOCH_MILLIS,
        incident: SessionIncident? = null,
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.TRIGGER_ELAPSED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = null,
        incident = incident,
    )

    internal fun invalidNfcScannedEvent(
        expectedRevision: Long,
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        occurredAtEpochMillis: Long = NFC_VERIFIED_AT_EPOCH_MILLIS,
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.INVALID_NFC_SCANNED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = null,
        incident = null,
    )

    internal fun releaseSucceededEvent(
        expectedRevision: Long,
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        occurredAtEpochMillis: Long = RELEASED_AT_EPOCH_MILLIS,
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.RELEASE_SUCCEEDED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = null,
        incident = null,
    )

    internal fun releaseFailedEvent(
        expectedRevision: Long,
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        occurredAtEpochMillis: Long = RELEASED_AT_EPOCH_MILLIS,
        incident: SessionIncident = incident(),
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.RELEASE_FAILED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = null,
        incident = incident,
    )

    internal fun incidentReportedEvent(
        expectedRevision: Long,
        sessionId: String = SESSION_ID,
        eventId: String = EVENT_ID_2,
        occurredAtEpochMillis: Long = ARMED_AT_EPOCH_MILLIS,
        incident: SessionIncident = incident(),
    ) = SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = SessionEventKind.INCIDENT_REPORTED,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = null,
        nfcProof = null,
        failureCode = null,
        incident = incident,
    )

    internal fun incident(
        code: String = IncidentCodes.MISSED_TRIGGER_WINDOW,
        severity: IncidentSeverity = IncidentSeverity.DEGRADED,
        occurredAtEpochMillis: Long = TRIGGER_ELAPSED_AT_EPOCH_MILLIS,
        platform: Platform = Platform.ANDROID,
    ) = SessionIncident(code, severity, occurredAtEpochMillis, platform)
}

/**
 * Scan NFC valide. La preuve doit reprendre exactement `sessionId`, `eventId`, `expectedRevision`
 * et `occurredAtEpochMillis` de l'événement qui la porte (SPEC_CORE_KMP §6, dernier alinéa) : elle
 * est donc dérivée de l'événement après sa construction plutôt que passée en paramètre indépendant,
 * ce qui rendrait un appelant capable de forger une incohérence par erreur.
 */
internal object NfcScanEventFixtures {
    internal fun validNfcScannedEvent(
        expectedRevision: Long,
        occurredAtEpochMillis: Long,
        sessionId: String = SESSION_ID,
        proofOverride: NfcVerificationProof? = null,
    ): SessionEvent {
        val shape =
            SessionEvent(
                eventId = EVENT_ID_2,
                sessionId = sessionId,
                kind = SessionEventKind.VALID_NFC_SCANNED,
                occurredAtEpochMillis = occurredAtEpochMillis,
                expectedRevision = expectedRevision,
                activationRequest = null,
                nfcProof = null,
                failureCode = null,
                incident = null,
            )
        return shape.copy(nfcProof = proofOverride ?: proofMatching(shape))
    }

    private fun proofMatching(event: SessionEvent) =
        NfcVerificationProof(
            boxId = BOX_ID,
            sessionId = event.sessionId,
            eventId = event.eventId,
            expectedRevision = requireNotNull(event.expectedRevision),
            verifiedAtEpochMillis = event.occurredAtEpochMillis,
        )
}
