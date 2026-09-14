package com.niumi.system.nfc

import com.niumi.core.interop.NfcVerificationContextDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.nfc.NfcVerificationProof
import com.niumi.system.common.Clock
import com.niumi.system.common.IdGenerator
import javax.inject.Inject

/**
 * Fabrique de `VALID_NFC_SCANNED` (SPEC_CORE_KMP §6, §9.2 ; SPEC_ANDROID §11.3 points 3 et 4).
 *
 * Séparée de `SessionEventFactory` pour une raison de fond et une raison mécanique. La raison de
 * fond : c'est le seul événement dont l'identité doit être frappée **avant** l'événement lui-même,
 * parce que la [NfcVerificationProof] lie `eventId`, `sessionId` et `expectedRevision` et n'est
 * délivrée que par la façade commune — d'où deux temps, [verificationContext] puis
 * [validNfcScanned], là où tous les autres événements se construisent d'un seul appel. La raison
 * mécanique : `SessionEventFactory` est déjà à onze fonctions, plafond `TooManyFunctions` de
 * detekt en classe comme en fichier.
 */
class NfcScanEventFactory
    @Inject
    constructor(
        private val idGenerator: IdGenerator,
        private val clock: Clock,
    ) {
        /**
         * Frappe l'identité du scan à venir. `expectedRevision` est la révision du snapshot **relu
         * après** l'éventuelle réconciliation d'avant-scan : le moteur exige l'égalité stricte
         * (`SessionEventValidation.revisionViolations`), contrairement à la garde de monotonie du
         * déclenchement d'alarme, dont l'extra de `PendingIntent` est figé bien plus tôt.
         */
        fun verificationContext(snapshot: SessionSnapshotDto): NfcVerificationContextDto =
            NfcVerificationContextDto(
                sessionId = snapshot.sessionId,
                eventId = idGenerator.newId(),
                expectedRevision = snapshot.revision,
                occurredAtEpochMillis = clock.nowEpochMillis(),
            )

        /**
         * [proof] ne peut venir que de `NiumiCoreFacade.verifyBox()` appelée avec ce même [context] :
         * `NfcReducer` rejette toute preuve dont un champ diffère de l'événement
         * (`UNEXPECTED_EVENT_PAYLOAD`).
         */
        fun validNfcScanned(
            context: NfcVerificationContextDto,
            proof: NfcVerificationProof,
        ): SessionEventDto =
            SessionEventDto(
                eventId = context.eventId,
                sessionId = context.sessionId,
                kind = SessionEventKindDto.VALID_NFC_SCANNED,
                occurredAtEpochMillis = context.occurredAtEpochMillis,
                expectedRevision = context.expectedRevision,
                activationRequest = null,
                nfcProof = proof,
                failureCode = null,
                incident = null,
            )
    }
