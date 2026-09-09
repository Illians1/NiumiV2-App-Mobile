package com.niumi.core.domain

/**
 * Codes de violation communs (SPEC_CORE_KMP §7.3, « au minimum les codes suivants »).
 * `INVALID_APP_SELECTION` est un ajout de l'étape 7 : §7.4 refuse une activation dont `count` est
 * hors de 1..50 mais aucun des 13 codes d'origine ne couvrait ce refus. Voir `ETAPE-07.md`.
 */
public object ViolationCode {
    public const val UNKNOWN_SESSION: String = "UNKNOWN_SESSION"
    public const val INVALID_STATE_TRANSITION: String = "INVALID_STATE_TRANSITION"
    public const val INVALID_IDENTIFIER: String = "INVALID_IDENTIFIER"
    public const val INVALID_TIMESTAMP: String = "INVALID_TIMESTAMP"
    public const val STALE_REVISION: String = "STALE_REVISION"
    public const val EVENT_ID_CONFLICT: String = "EVENT_ID_CONFLICT"
    public const val MISSING_ACTIVATION_REQUEST: String = "MISSING_ACTIVATION_REQUEST"
    public const val MISSING_FAILURE_CODE: String = "MISSING_FAILURE_CODE"
    public const val MISSING_INCIDENT: String = "MISSING_INCIDENT"
    public const val MISSING_NFC_PROOF: String = "MISSING_NFC_PROOF"
    public const val UNEXPECTED_EVENT_PAYLOAD: String = "UNEXPECTED_EVENT_PAYLOAD"
    public const val TRIGGER_NOT_REACHED: String = "TRIGGER_NOT_REACHED"
    public const val TRIGGER_ALREADY_ELAPSED: String = "TRIGGER_ALREADY_ELAPSED"
    public const val INVALID_APP_SELECTION: String = "INVALID_APP_SELECTION"
}
