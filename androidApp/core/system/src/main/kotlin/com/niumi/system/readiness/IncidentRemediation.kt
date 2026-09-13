package com.niumi.system.readiness

import com.niumi.core.domain.IncidentCodes
import com.niumi.system.notification.NiumiNotificationChannels

/**
 * Recours proposé pour un code d'incident (SPEC_ANDROID §15, « Remédiation des incidents sur
 * l'écran 7 » ; colonne « Action proposée » de §13).
 *
 * Table statique plutôt qu'une évaluation de [DeviceReadinessChecker] : l'action attachée à un code
 * ne dépend pas de l'état du système — un service d'accessibilité désactivé se répare toujours dans
 * les réglages d'accessibilité — et le checker est explicitement sans état, chaque appel relisant
 * ses sources. L'écran 7 a besoin du recours, pas d'un second diagnostic complet.
 *
 * C'est l'inverse de [MonitoredReadinessChecks.incidentCodes], à ceci près qu'elle couvre aussi les
 * codes qu'aucun contrôle de §13.1 ne produit (`NFC_DISABLED`). `null` signifie « aucun recours » :
 * un incident qui consigne un fait passé (`TIME_CHANGED`, `MISSED_TRIGGER_WINDOW`) ou un défaut
 * interne (`SNAPSHOT_CORRUPTED`, `RELEASE_PARTIAL_FAILURE`) ne se répare pas depuis un réglage
 * système, et lui attacher un bouton afficherait un faux état de fiabilité (§15).
 *
 * **Aucune de ces actions ne touche à la session.** Elles rétablissent un sous-système ; le scan du
 * boîtier reste le seul chemin de sortie (§3, §10.2).
 */
object IncidentRemediation {
    private val actions: Map<String, ReadinessAction> =
        mapOf(
            IncidentCodes.BLOCKING_PERMISSION_REVOKED to ReadinessAction.OpenAccessibilitySettings,
            // §13 : un accès aux alarmes exactes perdu n'ouvre aucun réglage — Niumi déclare
            // USE_EXACT_ALARM et ne présente jamais cet accès comme une permission ordinaire.
            IncidentCodes.ALARM_PERMISSION_REVOKED to ReadinessAction.ShowExactAlarmDiagnostic,
            IncidentCodes.NFC_DISABLED to ReadinessAction.OpenNfcSettings,
            AndroidIncidentCodes.ALARM_VOLUME_ZERO to ReadinessAction.OpenSoundSettings,
            AndroidIncidentCodes.ALARM_MUTED_BY_DND to ReadinessAction.OpenDndSettings,
            AndroidIncidentCodes.FULL_SCREEN_REVOKED to ReadinessAction.OpenFullScreenIntentSettings,
            AndroidIncidentCodes.NOTIFICATIONS_REVOKED to
                ReadinessAction.OpenChannelSettings(NiumiNotificationChannels.alarmRinging.id),
        )

    fun actionFor(code: String): ReadinessAction? = actions[code]
}
