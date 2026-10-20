package com.niumi.database.logging

/**
 * Liste blanche exacte des types d'événement techniques autorisés (SPEC_ANDROID §17). Aucun
 * type hors de cette liste ne peut être journalisé. `ALARM_MUTED_BY_DND` et
 * `SESSION_READINESS_DEGRADED` complétés à l'étape 9 : présents dans §17 et exigés nommément par
 * §13.1, absents de la liste livrée à l'étape 3 (voir `ETAPE-09.md`). `RINGTONE_FALLBACK` ajouté au
 * Lot 7 : une clé de sonnerie hors catalogue remplacée par la sonnerie par défaut.
 * `AUDIO_LOOP_FALLBACK` ajouté le 2026-10-15 : le lecteur sans blanc de boucle (`AudioTrack`) n'a pas
 * pu être créé, le réveil sonne par le repli `MediaPlayer`, avec un blanc à chaque tour.
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
    RINGTONE_FALLBACK,
    AUDIO_LOOP_FALLBACK,

    /**
     * Historique (Lot 9, 2026-10-18) : plus jamais émis depuis le retrait du plein écran. Conservé
     * parce que le journal relit ses lignes par `valueOf` : une ligne ancienne ferait sinon lever.
     */
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

    /** Début du blocage différé (Lot 6, SPEC_ANDROID §17). `packageName` y reste refusé. */
    BLOCKING_SCHEDULED,
    BLOCKING_START_RESCHEDULED,

    /**
     * Déclenchement de l'alarme de début reçu par `BlockingStartReceiver`, journalisé **avant toute
     * décision** — y compris quand la suite refuse d'appliquer le blocage. Pendant de
     * `ALARM_RECEIVED` pour le réveil : c'est ce qui rend visible un déclenchement orphelin, celui
     * d'une alarme qui a survécu à la fin de sa session parce que `CANCEL_BLOCKING_START`, effet
     * best-effort, a échoué. Sans lui, ce fait ne laisserait aucune trace exportable.
     */
    BLOCKING_START_RECEIVED,
    BLOCKING_STARTED,
    MISSED_BLOCKING_START_WINDOW,

    /**
     * Service de garde du blocage (SPEC_ANDROID §12.5, correctif A8 du 2026-10-19) : démarré quand le
     * blocage entre en vigueur, arrêté à la fin de la session, ou refusé par Android depuis
     * l'arrière-plan (`errorCode` contrôlé).
     */
    BLOCKING_GUARD_STARTED,
    BLOCKING_GUARD_STOPPED,
    BLOCKING_GUARD_START_REFUSED,
    ACCESSIBILITY_DISABLED,
    PROCESS_RECREATED,
    OEM_RESTRICTION_SUSPECTED,
    SESSION_COMPLETED,
    SESSION_FAILED,
    RELEASE_PARTIAL_FAILURE,

    /** Snapshot Direct Boot ou Room illisible (SPEC_CORE_KMP §13 ; SPEC_ANDROID §18, étape 20). */
    SNAPSHOT_CORRUPTED,
}
