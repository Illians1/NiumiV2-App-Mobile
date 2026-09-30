package com.niumi.system.readiness

import com.niumi.core.domain.IncidentCodes

/**
 * Codes d'incident propres à Android (SPEC_CORE_KMP §7.3 : chaque plateforme ajoute les siens
 * sous le préfixe `ANDROID_`, hors de `IncidentCodes` qui ne porte que le contrat commun).
 * Ceux-ci sont ceux du tableau de SPEC_ANDROID §13.1, tous de gravité `CRITICAL`.
 *
 * [BATTERY_EXEMPTION_REVOKED] (C9, 2026-09-29) : l'exemption d'énergie n'est surveillée que depuis
 * qu'elle est détectée (étape 25) ; avant, elle reposait sur une confirmation de l'utilisateur.
 */
object AndroidIncidentCodes {
    const val ALARM_MUTED_BY_DND = "ANDROID_ALARM_MUTED_BY_DND"
    const val ALARM_VOLUME_ZERO = "ANDROID_ALARM_VOLUME_ZERO"
    const val NOTIFICATIONS_REVOKED = "ANDROID_NOTIFICATIONS_REVOKED"
    const val FULL_SCREEN_REVOKED = "ANDROID_FULL_SCREEN_REVOKED"
    const val BATTERY_EXEMPTION_REVOKED = "ANDROID_BATTERY_EXEMPTION_REVOKED"
}

/**
 * Les sept contrôles surveillés pendant une session `ARMED` (SPEC_ANDROID §13.1) et le code
 * d'incident de chacun. Les huit autres contrôles de §13 ne sont pas surveillés : ils portent
 * sur le parcours de préparation, figé une fois la session armée, ou ne se retirent qu'à la main
 * (verrou dans les récents).
 *
 * **L'ordre compte** : il fixe l'identifiant de notification de chaque avertissement
 * (`SessionWarningNotificationSpecs.notificationId`). Un nouveau contrôle s'ajoute à la fin.
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
            ReadinessCheckId.BATTERY_OPTIMIZATION to AndroidIncidentCodes.BATTERY_EXEMPTION_REVOKED,
        )

    /**
     * Les deux contrôles qui restent surveillés après `ARMED`, parce qu'ils portent sur le
     * blocage et que le blocage court jusqu'au scan du boîtier, dans `RINGING`, `AWAITING_NFC`,
     * `TRIGGERED_AWAITING_NFC` et `RELEASING` (SPEC_ANDROID §3) :
     * - le service d'accessibilité (étape 15), dont §12.2 exige que la désactivation pendant une
     *   session soit détectée et présentée ;
     * - l'exemption d'énergie (C9, 2026-09-29) : sans elle, HyperOS gèle Niumi et le blocage cesse
     *   silencieusement après quelques minutes (§13, mesuré à l'étape 5).
     *
     * Les cinq autres contrôles portent sur le déclenchement du réveil : une fois la sonnerie
     * commencée, ils ne décrivent plus rien d'actionnable.
     */
    val blockingOnlyIncidentCodes: Map<ReadinessCheckId, String> =
        incidentCodes.filterKeys {
            it == ReadinessCheckId.ACCESSIBILITY_SERVICE || it == ReadinessCheckId.BATTERY_OPTIMIZATION
        }
}
