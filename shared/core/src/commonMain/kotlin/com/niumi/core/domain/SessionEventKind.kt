package com.niumi.core.domain

/** Nature d'un [SessionEvent] appliqué au réducteur (SPEC_CORE_KMP §6). */
public enum class SessionEventKind {
    ACTIVATION_REQUESTED,
    ACTIVATION_SUCCEEDED,
    ACTIVATION_FAILED,
    ALARM_FIRED,
    ALARM_SOUND_STOPPED,
    TRIGGER_ELAPSED,
    VALID_NFC_SCANNED,
    INVALID_NFC_SCANNED,
    RELEASE_SUCCEEDED,
    RELEASE_FAILED,
    INCIDENT_REPORTED,
}
