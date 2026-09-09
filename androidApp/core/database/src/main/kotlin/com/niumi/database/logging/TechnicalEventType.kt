package com.niumi.database.logging

/**
 * Liste blanche exacte des types d'événement techniques autorisés (SPEC_ANDROID §17). Aucun
 * type hors de cette liste ne peut être journalisé. `ALARM_MUTED_BY_DND` et
 * `SESSION_READINESS_DEGRADED` complétés à l'étape 9 : présents dans §17 et exigés nommément par
 * §13.1, absents de la liste livrée à l'étape 3 (voir `ETAPE-09.md`).
 */
enum class TechnicalEventType {
    SESSION_PREPARING,
    SESSION_ARMED,
    SESSION_RELEASING,
    SESSION_CANCELLED,
    ALARM_SCHEDULED,
    ALARM_RESCHEDULED,
    ALARM_RECEIVED,
    RINGING_STARTED,
    AUDIO_START_FAILED,
    FULL_SCREEN_DENIED,
    EXACT_ALARM_LOST,
    MISSED_TRIGGER_WINDOW,
    ALARM_MUTED_BY_DND,
    SESSION_READINESS_DEGRADED,
    SCAN_REQUEST_NOTIFIED,
    SCAN_REQUEST_CLEARED,
    NFC_DISABLED,
    NFC_SCAN_INVALID,
    NFC_SCAN_VALID,
    BLOCK_APPLIED,
    ACCESSIBILITY_DISABLED,
    PROCESS_RECREATED,
    OEM_RESTRICTION_SUSPECTED,
    SESSION_COMPLETED,
    SESSION_FAILED,
    RELEASE_PARTIAL_FAILURE,
}
