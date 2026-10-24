package com.niumi.core.domain

/**
 * États de la machine à états commune (SPEC_CORE_KMP §5). `DISARMED` (contrat 1.5) est l'état final
 * d'une session désarmée sans scan avant son coucher, préalable à un ré-armement du planning.
 */
public enum class SessionState {
    PREPARING,
    ARMED,
    RINGING,
    AWAITING_NFC,
    TRIGGERED_AWAITING_NFC,
    RELEASING,
    COMPLETED,
    CANCELLED,
    DISARMED,
    FAILED,
}
