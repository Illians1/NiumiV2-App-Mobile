package com.niumi.system.session.fakes

import com.niumi.core.interop.ActivationRequestDto
import com.niumi.core.interop.AppSelectionSummaryDto
import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.BoxPayloadDto
import com.niumi.core.interop.NfcVerificationContextDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.core.nfc.NfcVerificationProof
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage

/**
 * Fixtures partagées par les tests du coordinateur : les fixtures de `:shared:core` sont
 * `internal` à `commonTest`, inaccessibles depuis Android (voir plan étape 11).
 */
object SessionDtoFixtures {
    const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
    const val OTHER_SESSION_ID = "99999999-9999-9999-9999-999999999999"
    const val BOX_ID = "22222222-2222-2222-2222-222222222222"

    // Bien après tout `occurredAtEpochMillis` utilisé dans les tests : évite `TRIGGER_ALREADY_ELAPSED`
    // et `MISSED`/`FIRE_NOW` non désirés dans les scénarios d'activation et de libération.
    const val TRIGGER_AT_EPOCH_MILLIS = 2_000_000_000_000L

    private val wakeSchedule =
        WakeScheduleDto(
            localDateIso = "2026-09-10",
            localTimeIso = "07:00",
            zoneIdAtActivation = "Europe/Paris",
            triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
        )

    fun extras(
        blockedPackages: List<BlockedPackage> = listOf(BlockedPackage("com.example.app", "Exemple")),
    ): AndroidSessionExtras =
        AndroidSessionExtras(
            boxId = BOX_ID,
            boxTokenSha256Hex = "a".repeat(64),
            ringtoneKey = "niumi_default",
            vibrationEnabled = true,
            blockedPackages = blockedPackages,
        )

    /**
     * Début d'un blocage différé, strictement antérieur au réveil (SPEC_CORE_KMP §8.3) et
     * postérieur aux `occurredAtEpochMillis` des tests, pour que l'activation produise bien
     * `SCHEDULE_BLOCKING_START` et non `APPLY_BLOCKING` (§8.3, instant déjà dépassé).
     */
    const val BLOCKING_STARTS_AT_EPOCH_MILLIS = 1_999_000_000_000L

    private val blockingSchedule =
        BlockingScheduleDto(
            localDateIso = "2026-09-09",
            localTimeIso = "22:30",
            startsAtEpochMillis = BLOCKING_STARTS_AT_EPOCH_MILLIS,
        )

    /**
     * [blockingStartsAtEpochMillis] nul laisse le blocage **immédiat**, ce qu'Android arme
     * exclusivement avant l'écran de l'étape 24 : les tests hérités décrivent donc toujours le même
     * parcours, ordinaux et `effectId` compris.
     */
    fun activationRequested(
        eventId: String,
        sessionId: String = SESSION_ID,
        occurredAtEpochMillis: Long = 1_000L,
        appCount: Int = 2,
        blockingStartsAtEpochMillis: Long? = null,
    ): SessionEventDto =
        SessionEventDto(
            eventId = eventId,
            sessionId = sessionId,
            kind = SessionEventKindDto.ACTIVATION_REQUESTED,
            occurredAtEpochMillis = occurredAtEpochMillis,
            expectedRevision = null,
            activationRequest =
                ActivationRequestDto(
                    wakeSchedule,
                    AppSelectionSummaryDto(appCount),
                    blockingSchedule =
                        blockingStartsAtEpochMillis
                            ?.let { blockingSchedule.copy(startsAtEpochMillis = it) }
                            ?: BlockingScheduleDto(),
                ),
            failureCode = null,
            incident = null,
        )

    /**
     * `VALID_NFC_SCANNED` exige une [NfcVerificationProof] opaque, constructible uniquement via
     * `NiumiCoreFacade.verifyBox()` (SPEC_CORE_KMP §6, §14) : jamais un mock direct.
     */
    fun validNfcScanned(
        facade: NiumiCoreFacade,
        eventId: String,
        sessionId: String = SESSION_ID,
        expectedRevision: Long,
        occurredAtEpochMillis: Long,
    ): SessionEventDto {
        val payload = BoxPayloadDto(protocolVersion = 1, boxId = BOX_ID, tokenBytes = byteArrayOf(1, 2, 3, 4))
        val credential = PairedBoxCredentialDto.fromPayload(payload)
        val context = NfcVerificationContextDto(sessionId, eventId, expectedRevision, occurredAtEpochMillis)
        val proof =
            requireNotNull(facade.verifyBox(payload, credential, context).proof) {
                "verifyBox() n'a pas produit de preuve : payload/credential désynchronisés dans la fixture"
            }
        return SessionEventDto(
            eventId = eventId,
            sessionId = sessionId,
            kind = SessionEventKindDto.VALID_NFC_SCANNED,
            occurredAtEpochMillis = occurredAtEpochMillis,
            expectedRevision = expectedRevision,
            activationRequest = null,
            nfcProof = proof,
            failureCode = null,
            incident = null,
        )
    }

    /**
     * Snapshot synthétique dans l'état demandé, pour les appelants qui n'ont besoin que de
     * l'état et de la révision (abonnements au publisher, surveillance §13.1). Les horodatages
     * ne sont pas rendus cohérents avec l'état : seul le moteur KMP en est responsable, et
     * aucun consommateur de cette fixture ne les lit.
     */
    fun snapshotInState(
        state: SessionStateDto,
        sessionId: String = SESSION_ID,
        revision: Long = 1,
    ): SessionSnapshotDto = releasingSnapshot(sessionId, revision).copy(state = state)

    /**
     * Snapshot `ARMED` à blocage différé (Lot 6). [blockingAppliedAtEpochMillis] nul décrit une
     * session encore en attente de son instant de début — le seul cas où `isBlockingPending` vaut
     * vrai. [startsAtEpochMillis] nul décrit une session à blocage immédiat, pour les tests qui
     * doivent prouver qu'un effet différé n'est jamais produit dans ce cas.
     */
    fun deferredArmedSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 2,
        startsAtEpochMillis: Long? = BLOCKING_STARTS_AT_EPOCH_MILLIS,
        blockingAppliedAtEpochMillis: Long? = null,
    ): SessionSnapshotDto =
        snapshotInState(SessionStateDto.ARMED, sessionId, revision).copy(
            schemaVersion = 2,
            releaseTarget = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            blockingSchedule =
                startsAtEpochMillis
                    ?.let { blockingSchedule.copy(startsAtEpochMillis = it) }
                    ?: BlockingScheduleDto(),
            blockingAppliedAtEpochMillis = blockingAppliedAtEpochMillis,
        )

    /** Snapshot `RELEASING` synthétique, pour les tests de rejeu d'outbox (réconciliateur). */
    fun releasingSnapshot(
        sessionId: String = SESSION_ID,
        revision: Long = 1,
    ): SessionSnapshotDto =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = revision,
            sessionId = sessionId,
            wakeSchedule = wakeSchedule,
            state = SessionStateDto.RELEASING,
            releaseTarget = ReleaseTargetDto.CANCELLED,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1_000L,
            armedAtEpochMillis = 1_100L,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = 1_200L,
            releasingAtEpochMillis = 1_200L,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )
}
