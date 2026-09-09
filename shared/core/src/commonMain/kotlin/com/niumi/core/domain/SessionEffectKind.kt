package com.niumi.core.domain

/** Effet natif à exécuter par le coordinateur, sans appel système direct (SPEC_CORE_KMP §6). */
public enum class SessionEffectKind {
    PUBLISH_PLATFORM_SNAPSHOT,
    SCHEDULE_ALARM,
    CANCEL_ALARM,
    APPLY_BLOCKING,
    REMOVE_BLOCKING,
    START_RINGING,
    STOP_RINGING,
    PRESENT_SCAN_REQUEST,
    CLEAR_SCAN_REQUEST,
    CLEAR_ACTIVE_SESSION,
    RECORD_INCIDENT,
}
