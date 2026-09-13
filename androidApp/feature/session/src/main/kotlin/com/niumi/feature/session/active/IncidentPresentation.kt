package com.niumi.feature.session.active

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.system.readiness.IncidentRemediation
import com.niumi.system.readiness.ReadinessAction

/**
 * Un incident tel que l'écran 7 le présente : le fait métier, plus le recours éventuel
 * (SPEC_ANDROID §15, « Remédiation des incidents sur l'écran 7 »).
 *
 * [action] et [actionLabel] sont nuls ou non nuls ensemble, par construction ([of]) : un bouton
 * sans destination promettrait une action qui n'existe pas, ce que §15 interdit (« ne jamais
 * afficher un faux état de fiabilité »). Un recours que §13 décrit sans réglage système à ouvrir —
 * `ShowExactAlarmDiagnostic` — n'en produit donc aucun ici : le libellé de l'incident porte déjà
 * l'explication.
 *
 * [code] et [severity] sont délégués pour que la présentation se lise comme l'incident qu'elle
 * enrobe.
 */
data class IncidentPresentation(
    val incident: SessionIncidentDto,
    val action: ReadinessAction? = null,
    val actionLabel: String? = null,
) {
    val code: String get() = incident.code

    val severity: IncidentSeverityDto get() = incident.severity

    companion object {
        fun of(incident: SessionIncidentDto): IncidentPresentation {
            val action = IncidentRemediation.actionFor(incident.code)
            val label = action?.let(ActiveSessionTexts::actionLabel)
            return IncidentPresentation(
                incident = incident,
                action = action.takeIf { label != null },
                actionLabel = label,
            )
        }
    }
}
