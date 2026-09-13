package com.niumi.feature.ringing.fakes

import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.system.common.OperationResult
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway

const val TEST_SESSION_ID = "11111111-1111-1111-1111-111111111111"

/**
 * Le dépôt n'a pas de `testFixtures` : chaque module redéfinit ses doubles (convention posée à
 * l'étape 15). Celui-ci ne répond qu'aux lectures — toute écriture lève, ce qui **prouve** qu'un
 * écran n'écrit jamais dans la session (SPEC_ANDROID §3 : le scan est le seul chemin de sortie).
 */
class FakeSessionPersistenceGateway(
    var result: LoadResult = LoadResult.Absent,
    var replayableEffects: List<PendingEffect> = emptyList(),
) : SessionPersistenceGateway {
    override suspend fun load(): LoadResult = result

    override suspend fun pendingEffects(sessionId: String): List<PendingEffect> = replayableEffects

    override suspend fun commit(decision: StoredDecision) = error("COMMIT_FROM_SCREEN")

    override suspend fun receipt(eventId: String): EventReceipt? = error("RECEIPT_FROM_SCREEN")

    override suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) = error("MARK_EFFECT_FROM_SCREEN")

    override suspend fun clearActive(sessionId: String) = error("CLEAR_ACTIVE_FROM_SCREEN")

    override suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ): OperationResult = error("RECORD_INCIDENT_FROM_SCREEN")
}

fun snapshotInState(
    state: SessionStateDto,
    revision: Long = 2,
): SessionSnapshotDto =
    SessionSnapshotDto(
        schemaVersion = 1,
        revision = revision,
        sessionId = TEST_SESSION_ID,
        wakeSchedule = WakeScheduleDto("2026-09-13", "07:00", "Europe/Paris", 2_000_000_000_000L),
        state = state,
        releaseTarget = null,
        health = SessionHealthDto.HEALTHY,
        createdAtEpochMillis = 1_000L,
        armedAtEpochMillis = 1_100L,
        ringingAtEpochMillis = null,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = null,
        nfcVerifiedAtEpochMillis = null,
        releasingAtEpochMillis = null,
        completedAtEpochMillis = null,
        cancelledAtEpochMillis = null,
        failureCode = null,
    )

fun presentSession(
    state: SessionStateDto,
    pendingEffects: List<PendingEffect> = emptyList(),
): LoadResult.Present =
    LoadResult.Present(
        snapshot = snapshotInState(state),
        extras =
            AndroidSessionExtras(
                boxId = "22222222-2222-2222-2222-222222222222",
                boxTokenSha256Hex = "a".repeat(64),
                ringtoneKey = "niumi_default",
                vibrationEnabled = true,
                blockedPackages = listOf(BlockedPackage("com.example.app", "Exemple")),
            ),
        pendingEffects = pendingEffects,
    )

fun pendingEffect(kind: SessionEffectKindDto): PendingEffect =
    PendingEffect(
        effectId = "$TEST_SESSION_ID:3:$kind:0",
        sessionId = TEST_SESSION_ID,
        revision = 3,
        kind = kind,
        ordinal = 0,
        payloadJson = null,
        status = EffectStatus.PENDING,
        lastError = null,
    )
