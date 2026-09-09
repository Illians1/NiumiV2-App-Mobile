package com.niumi.core.domain

import com.niumi.core.nfc.NfcVerificationProof

/**
 * Événement soumis au réducteur (SPEC_CORE_KMP §6). `expectedRevision` est obligatoire sauf pour
 * `ACTIVATION_REQUESTED`. `activationRequest` n'est attendu que pour `ACTIVATION_REQUESTED`,
 * `nfcProof` que pour `VALID_NFC_SCANNED`, `failureCode` que pour `ACTIVATION_FAILED`, `incident`
 * pour `RELEASE_FAILED` et `INCIDENT_REPORTED` (facultatif pour `TRIGGER_ELAPSED`, afin d'y joindre
 * `MISSED_TRIGGER_WINDOW`). Un champ présent hors de son événement produit la violation
 * `UNEXPECTED_EVENT_PAYLOAD` : aucun constructeur ne valide, voir [SessionEventValidation]
 * (§14 interdit toute exception traversant la frontière native).
 */
public data class SessionEvent(
    val eventId: String,
    val sessionId: String,
    val kind: SessionEventKind,
    val occurredAtEpochMillis: Long,
    val expectedRevision: Long?,
    val activationRequest: ActivationRequest?,
    val nfcProof: NfcVerificationProof?,
    val failureCode: String?,
    val incident: SessionIncident?,
)
