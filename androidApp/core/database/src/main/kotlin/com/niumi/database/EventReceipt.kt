package com.niumi.database

/**
 * Reçu d'un événement déjà appliqué (SPEC_CORE_KMP §6.1) : `eventId` identifie l'événement,
 * `payloadSha256Hex` est son empreinte canonique (voir `EventFingerprint`), `appliedRevision` la
 * révision produite. Un `eventId` déjà reçu avec une empreinte différente produit
 * `EVENT_ID_CONFLICT` ; avec la même empreinte, l'événement est reconnu sans nouvel effet.
 */
data class EventReceipt(
    val eventId: String,
    val sessionId: String,
    val payloadSha256Hex: String,
    val appliedRevision: Long,
    val receivedAtEpochMillis: Long,
)
