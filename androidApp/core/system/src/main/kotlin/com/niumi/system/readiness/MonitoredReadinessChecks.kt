package com.niumi.system.readiness

import com.niumi.core.domain.IncidentCodes

/**
 * Codes d'incident propres à Android (SPEC_CORE_KMP §7.3 : chaque plateforme ajoute les siens
 * sous le préfixe `ANDROID_`, hors de `IncidentCodes` qui ne porte que le contrat commun).
 * Ceux-ci sont ceux du tableau de SPEC_ANDROID §13.1, tous de gravité `CRITICAL`.
 */
object AndroidIncidentCodes {
    const val ALARM_MUTED_BY_DND = "ANDROID_ALARM_MUTED_BY_DND"
    const val ALARM_VOLUME_ZERO = "ANDROID_ALARM_VOLUME_ZERO"
    const val NOTIFICATIONS_REVOKED = "ANDROID_NOTIFICATIONS_REVOKED"
    const val FULL_SCREEN_REVOKED = "ANDROID_FULL_SCREEN_REVOKED"
}

/**
 * Les six contrôles surveillés pendant une session `ARMED` (SPEC_ANDROID §13.1) et le code
 * d'incident de chacun. Les huit autres contrôles de §13 ne sont pas surveillés : ils portent
 * sur le parcours de préparation, figé une fois la session armée.
 *
 * Deux codes sont volontairement **communs** et non préfixés `ANDROID_` (§7.1) : une perte de
 * permission doit rester comparable entre Android et iOS.
 */
object MonitoredReadinessChecks {
    val incidentCodes: Map<ReadinessCheckId, String> =
        linkedMapOf(
            ReadinessCheckId.EXACT_ALARM to IncidentCodes.ALARM_PERMISSION_REVOKED,
            ReadinessCheckId.FULL_SCREEN_INTENT to AndroidIncidentCodes.FULL_SCREEN_REVOKED,
            ReadinessCheckId.NOTIFICATIONS to AndroidIncidentCodes.NOTIFICATIONS_REVOKED,
            ReadinessCheckId.ALARM_VOLUME to AndroidIncidentCodes.ALARM_VOLUME_ZERO,
            ReadinessCheckId.DND_TOTAL_SILENCE to AndroidIncidentCodes.ALARM_MUTED_BY_DND,
            ReadinessCheckId.ACCESSIBILITY_SERVICE to IncidentCodes.BLOCKING_PERMISSION_REVOKED,
        )
}
