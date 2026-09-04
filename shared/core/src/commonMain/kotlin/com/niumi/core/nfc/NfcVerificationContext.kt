package com.niumi.core.nfc

/**
 * Contexte de la transition en cours, fourni par le coordinateur natif avant de vérifier un scan
 * (SPEC_CORE_KMP §6). Lie la vérification à un unique événement `VALID_NFC_SCANNED`. `null` lors
 * d'une association, où aucune session n'existe encore.
 */
public data class NfcVerificationContext(
    val sessionId: String,
    val eventId: String,
    val expectedRevision: Long,
    val occurredAtEpochMillis: Long,
)
