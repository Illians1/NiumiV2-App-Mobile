package com.niumi.feature.session.diagnostics

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.feature.session.incident.IncidentTexts
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome

/**
 * Textes de l'écran 12 — diagnostic d'incident (SPEC_ANDROID §15, §17, §18). Tutoiement (§15).
 *
 * Les contrôles y sont **nommés**, pas expliqués comme sur l'écran 2. Les messages de §13
 * (`ReadinessMessages`, `:feature:setup`) sont rédigés comme des consignes d'activation — « Active-le
 * avant de démarrer la session » — et seraient faux pendant une session déjà armée, qu'ils
 * inviteraient à préparer une seconde fois. L'écran 12 constate ; l'écran 7 porte le recours.
 */
object IncidentDiagnosticTexts {
    const val TITLE = "Diagnostic"

    /** Étape 20 : Room et Direct Boot illisibles à la fois, ou Room seul après déverrouillage. */
    const val STORAGE_FAILURE_TITLE = "Niumi ne peut plus lire son état enregistré"

    const val STORAGE_FAILURE_MESSAGE =
        "Tes applications restent bloquées. Scanne ton boîtier pour terminer ta session."

    const val EXPORT_TITLE = "Niumi — diagnostic"

    const val EXPORT_BUTTON = "Exporter le diagnostic"

    /** §17 : l'export part après une action explicite de l'utilisateur, jamais tout seul. */
    const val EXPORT_CHOOSER_TITLE = "Partager le diagnostic"

    const val SESSION_TITLE = "Session"

    const val NO_SESSION = "Aucune session active."

    const val BOX_TITLE = "Boîtier"

    const val CHECKS_TITLE = "Contrôles"

    const val INCIDENTS_TITLE = "Incidents"

    const val NO_INCIDENT = "Aucun incident."

    const val EVENTS_TITLE = "Événements"

    const val NO_EVENT = "Aucun événement."

    const val OUTCOME_PASSED = "OK"

    const val OUTCOME_FAILED = "en échec"

    const val OUTCOME_NOT_APPLICABLE = "sans objet"

    /**
     * §7.3 (KMP) : une session `DEGRADED` ne redevient jamais `HEALTHY` d'elle-même. Le texte ne
     * promet donc aucun retour à la normale.
     */
    fun healthLabel(health: SessionHealthDto): String =
        when (health) {
            SessionHealthDto.HEALTHY -> "Normale"
            SessionHealthDto.DEGRADED -> "Dégradée"
        }

    fun stateLabel(state: SessionStateDto): String = state.name

    fun outcomeLabel(outcome: ReadinessOutcome): String =
        when (outcome) {
            ReadinessOutcome.PASSED -> OUTCOME_PASSED
            ReadinessOutcome.FAILED -> OUTCOME_FAILED
            ReadinessOutcome.NOT_APPLICABLE -> OUTCOME_NOT_APPLICABLE
        }

    fun severityLabel(severity: IncidentSeverityDto): String = IncidentTexts.severityLabel(severity)

    /**
     * Un incident nommé en clair, suivi de son code technique. Les deux : §7.3 (KMP) veut un
     * diagnostic « visible par l'utilisateur », et §18 veut qu'« une erreur inconnue reçoive un
     * identifiant local consultable dans le diagnostic ». Le code seul était opaque (mesuré sur
     * appareil, étape 16) ; le libellé seul priverait l'assistance de l'identifiant.
     */
    fun incidentLabel(code: String): String {
        val label = IncidentTexts.label(code)
        return if (label == code) code else "$label ($code)"
    }

    /** Les quatorze contrôles de §13, nommés dans l'ordre du tableau. */
    fun checkLabel(id: ReadinessCheckId): String =
        when (id) {
            ReadinessCheckId.NFC_PRESENT -> "NFC présent"
            ReadinessCheckId.NFC_ENABLED -> "NFC activé"
            ReadinessCheckId.PAIRED_BOX -> "Boîtier associé"
            ReadinessCheckId.APP_SELECTION -> "Applications choisies"
            ReadinessCheckId.EXACT_ALARM -> "Alarme exacte disponible"
            ReadinessCheckId.FULL_SCREEN_INTENT -> "Alarme plein écran autorisée"
            ReadinessCheckId.NOTIFICATIONS -> "Notifications autorisées"
            ReadinessCheckId.ALARM_CHANNEL -> "Canal d'alarme actif"
            ReadinessCheckId.ALARM_VOLUME -> "Volume des alarmes"
            ReadinessCheckId.DND_TOTAL_SILENCE -> "Ne pas déranger en silence total"
            ReadinessCheckId.DND_OTHER_MODE -> "Ne pas déranger dans un autre mode"
            ReadinessCheckId.ACCESSIBILITY_SERVICE -> "Service d'accessibilité actif"
            ReadinessCheckId.FUTURE_TRIGGER -> "Heure de réveil future"
            ReadinessCheckId.BATTERY_OPTIMIZATION -> "Restrictions de batterie levées"
        }
}
