package com.niumi.core.domain

/**
 * Codes d'incident communs et leur gravité par défaut (SPEC_CORE_KMP §7.3). Chaque plateforme peut
 * ajouter des codes préfixés `ANDROID_` ou `IOS_`, hors de cet objet.
 */
public object IncidentCodes {
    public const val ALARM_PERMISSION_REVOKED: String = "ALARM_PERMISSION_REVOKED"
    public const val BLOCKING_PERMISSION_REVOKED: String = "BLOCKING_PERMISSION_REVOKED"
    public const val NFC_DISABLED: String = "NFC_DISABLED"
    public const val TIME_CHANGED: String = "TIME_CHANGED"
    public const val TIMEZONE_CHANGED: String = "TIMEZONE_CHANGED"
    public const val MISSED_TRIGGER_WINDOW: String = "MISSED_TRIGGER_WINDOW"
    public const val PROCESS_RECREATED: String = "PROCESS_RECREATED"
    public const val RELEASE_PARTIAL_FAILURE: String = "RELEASE_PARTIAL_FAILURE"
    public const val SNAPSHOT_CORRUPTED: String = "SNAPSHOT_CORRUPTED"

    /** Gravité par défaut d'un code commun, `null` pour un code inconnu ou préfixé par plateforme. */
    public fun defaultSeverityOf(code: String): IncidentSeverity? =
        when (code) {
            ALARM_PERMISSION_REVOKED, BLOCKING_PERMISSION_REVOKED, NFC_DISABLED, SNAPSHOT_CORRUPTED -> {
                IncidentSeverity.CRITICAL
            }

            MISSED_TRIGGER_WINDOW, RELEASE_PARTIAL_FAILURE -> {
                IncidentSeverity.DEGRADED
            }

            TIME_CHANGED, TIMEZONE_CHANGED, PROCESS_RECREATED -> {
                IncidentSeverity.WARNING
            }

            else -> {
                null
            }
        }
}
