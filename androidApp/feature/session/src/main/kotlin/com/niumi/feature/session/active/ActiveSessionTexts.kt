package com.niumi.feature.session.active

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.feature.session.incident.IncidentTexts
import com.niumi.system.readiness.ReadinessAction

/** Textes de l'écran 7 (SPEC_ANDROID §15, écran 7), complets depuis l'étape 15. Tutoiement (§15). */
object ActiveSessionTexts {
    const val TITLE = "Ta session est active"

    const val NO_SESSION = "Aucune session active."

    const val CURRENT_ZONE_TITLE = "Dans ton fuseau actuel"

    const val COMMITMENT_REMINDER = "Seul le scan du boîtier terminera la session."

    const val BLOCKED_APPS_TITLE = "Applications bloquées"

    /** Ligne du début de blocage tant qu'il n'est pas atteint (Lot 6, §15, écran 7). */
    const val BLOCKING_START_TITLE = "Début du blocage"

    /**
     * « Un titre qui dit la vérité » (§15) : avant l'instant de début, la liste décrit ce qui
     * *sera* bloqué. Elle reste affichée dans les deux cas — l'engagement, lui, est déjà pris.
     */
    fun blockedAppsTitle(isBlockingPending: Boolean): String =
        if (isBlockingPending) "Applications qui seront bloquées" else BLOCKED_APPS_TITLE

    const val NO_BLOCKED_APP = "Aucune application bloquée pour cette session."

    const val HEALTH_TITLE = "État de la session"

    const val HEALTHY = "Tout fonctionne normalement."

    /**
     * §15 : « ne jamais afficher un faux état de fiabilité ». Une session `DEGRADED` ne redevient
     * jamais `HEALTHY` d'elle-même (SPEC_CORE_KMP §7.3), le texte ne promet donc aucun retour à la
     * normale.
     */
    const val DEGRADED = "Un incident a affecté cette session. Ton réveil peut être moins fiable."

    const val INCIDENTS_TITLE = "Incidents"

    const val CRITICAL_INCIDENTS_TITLE = "À vérifier maintenant"

    /** Écran 9 : le seul chemin de sortie d'une session active (SPEC_CORE_KMP §2, points 3 et 4). */
    const val MODIFY_OR_CANCEL_BUTTON = "Modifier ou annuler"

    /** Écran 12, en consultation : ne termine ni ne modifie la session (§15, étape 16). */
    const val OPEN_DIAGNOSTIC_BUTTON = "Voir le diagnostic"

    /**
     * Un état métier nommé en clair. `RINGING` et les trois états d'attente de scan ont leur propre
     * écran (§10.4) ; ils ne sont listés ici que pour n'afficher jamais un état inconnu.
     *
     * `ARMED` se dédouble depuis le blocage différé (Lot 6, §15) : [blockingTimeLabel] non nul
     * signifie « le blocage n'a pas encore commencé, et voici quand il commencera ». Aucun autre
     * état ne porte cette heure — un réveil qui sonne bloque déjà (point de vigilance 13).
     */
    fun stateLabel(
        state: SessionStateDto,
        blockingTimeLabel: String? = null,
    ): String =
        when (state) {
            SessionStateDto.PREPARING -> {
                "Activation en cours"
            }

            SessionStateDto.ARMED -> {
                if (blockingTimeLabel == null) {
                    "Réveil programmé · applications bloquées"
                } else {
                    "Réveil programmé · blocage à $blockingTimeLabel"
                }
            }

            SessionStateDto.RINGING -> {
                "Ton réveil sonne"
            }

            SessionStateDto.AWAITING_NFC -> {
                "En attente du scan de ton boîtier"
            }

            SessionStateDto.TRIGGERED_AWAITING_NFC -> {
                "En attente du scan de ton boîtier"
            }

            SessionStateDto.RELEASING -> {
                "Déblocage en cours"
            }

            SessionStateDto.COMPLETED -> {
                "Session terminée"
            }

            SessionStateDto.CANCELLED -> {
                "Session annulée"
            }

            SessionStateDto.FAILED -> {
                "L'activation a échoué"
            }
        }

    /** Délègue à [IncidentTexts] : l'écran 12 nomme les mêmes incidents (étape 16). */
    fun severityLabel(severity: IncidentSeverityDto): String = IncidentTexts.severityLabel(severity)

    /** Délègue à [IncidentTexts] : l'écran 12 nomme les mêmes incidents (étape 16). */
    fun incidentLabel(code: String): String = IncidentTexts.label(code)

    /**
     * Libellé du bouton de remédiation d'un incident (§15). `null` signifie « pas de bouton » :
     * aucun de ces recours n'ouvre de réglage système atteignable depuis l'écran 7 — l'association
     * et le sélecteur sont bloqués pendant une session (§10.4), la permission de notification passe
     * par un lanceur qui n'a pas sa place ici, et un accès aux alarmes exactes perdu n'a, par §13,
     * qu'une explication à offrir. Le libellé de l'incident la porte déjà.
     *
     * « Ouvrir les réglages d'accessibilité » est imposé mot pour mot par §15.
     */
    fun actionLabel(action: ReadinessAction): String? =
        when (action) {
            ReadinessAction.OpenAccessibilitySettings -> "Ouvrir les réglages d'accessibilité"

            ReadinessAction.OpenSoundSettings -> "Ouvrir les réglages du son"

            ReadinessAction.OpenDndSettings -> "Ouvrir les réglages Ne pas déranger"

            ReadinessAction.OpenFullScreenIntentSettings -> "Ouvrir les réglages d'alarme plein écran"

            is ReadinessAction.OpenChannelSettings -> "Ouvrir les réglages de notification"

            ReadinessAction.OpenNfcSettings -> "Ouvrir les réglages NFC"

            ReadinessAction.ShowExactAlarmDiagnostic,
            ReadinessAction.StartPairing,
            ReadinessAction.OpenAppPicker,
            ReadinessAction.RequestNotificationPermission,
            ReadinessAction.FixTime,
            ReadinessAction.Unsupported,
            is ReadinessAction.OpenBatterySettings,
            -> null
        }
}
