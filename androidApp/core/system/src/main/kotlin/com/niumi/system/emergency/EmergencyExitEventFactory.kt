package com.niumi.system.emergency

import com.niumi.core.emergency.EmergencyExitProof
import com.niumi.core.interop.NfcVerificationContextDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.system.common.Clock
import com.niumi.system.common.IdGenerator
import javax.inject.Inject

/**
 * Fabrique de `EMERGENCY_EXIT_CONFIRMED` (SPEC_CORE_KMP §5.1, §12 ; étape 44). Même découpage en deux
 * temps que `NfcScanEventFactory`, pour la même raison : l'[EmergencyExitProof] lie `sessionId`,
 * `eventId`, `expectedRevision` et l'instant de vérification, et n'est délivrée que par
 * `NiumiCoreFacade.verifyEmergencyTranscription` — l'identité de l'événement doit donc être frappée
 * avant la vérification de la recopie. Le contexte reprend le type de celui du scan, comme la façade.
 */
class EmergencyExitEventFactory
    @Inject
    constructor(
        private val idGenerator: IdGenerator,
        private val clock: Clock,
    ) {
        /** `expectedRevision` : révision du snapshot relu juste avant la vérification (égalité stricte). */
        fun verificationContext(snapshot: SessionSnapshotDto): NfcVerificationContextDto =
            NfcVerificationContextDto(
                sessionId = snapshot.sessionId,
                eventId = idGenerator.newId(),
                expectedRevision = snapshot.revision,
                occurredAtEpochMillis = clock.nowEpochMillis(),
            )

        /** [proof] doit venir de la façade appelée avec ce même [context], sinon `UNEXPECTED_EVENT_PAYLOAD`. */
        fun emergencyExitConfirmed(
            context: NfcVerificationContextDto,
            proof: EmergencyExitProof,
        ): SessionEventDto =
            SessionEventDto(
                eventId = context.eventId,
                sessionId = context.sessionId,
                kind = SessionEventKindDto.EMERGENCY_EXIT_CONFIRMED,
                occurredAtEpochMillis = context.occurredAtEpochMillis,
                expectedRevision = context.expectedRevision,
                activationRequest = null,
                failureCode = null,
                incident = null,
                emergencyProof = proof,
            )
    }
