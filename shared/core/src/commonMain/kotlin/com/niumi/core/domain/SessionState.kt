package com.niumi.core.domain

/** États de la machine à états commune (SPEC_CORE_KMP §5). */
public enum class SessionState {
    PREPARING,
    ARMED,
    RINGING,
    AWAITING_NFC,
    TRIGGERED_AWAITING_NFC,
    RELEASING,
    COMPLETED,
    CANCELLED,
    FAILED,
}
