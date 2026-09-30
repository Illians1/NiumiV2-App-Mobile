package com.niumi.feature.session.active

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.system.readiness.IncidentReadinessChecks
import com.niumi.system.readiness.IncidentRemediation
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.ReadinessReport

/**
 * Un incident tel que l'écran 7 le présente : le fait métier, le recours éventuel (SPEC_ANDROID
 * §15, « Remédiation des incidents sur l'écran 7 ») et, depuis l'étape 25, s'il est **rétabli**.
 *
 * [action] et [actionLabel] sont nuls ou non nuls ensemble, par construction ([of]) : un bouton
 * sans destination promettrait une action qui n'existe pas, ce que §15 interdit (« ne jamais
 * afficher un faux état de fiabilité »). Un recours que §13 décrit sans réglage système à ouvrir —
 * `ShowExactAlarmDiagnostic` — n'en produit donc aucun ici : le libellé de l'incident porte déjà
 * l'explication.
 *
 * [resolved] : le contrôle de §13 qui juge la cause de l'incident ([IncidentReadinessChecks]) est
 * repassé `PASSED` dans le rapport rejoué à l'affichage. Un incident rétabli garde sa gravité — le
 * fait a eu lieu, la santé reste `DEGRADED` (SPEC_CORE_KMP §7.3) — mais quitte le bloc « À vérifier
 * maintenant » et perd son recours : inviter à ouvrir un réglage déjà rétabli serait le faux état
 * que §15 interdit. Mesuré le 2026-09-25 : l'écran demandait de réactiver un service d'accessibilité
 * déjà actif, blocage appliqué. Sans rapport, sans contrôle pour ce code, ou pour un contrôle
 * `NOT_APPLICABLE`, rien n'est présumé : l'incident reste à vérifier.
 *
 * [code] et [severity] sont délégués pour que la présentation se lise comme l'incident qu'elle
 * enrobe.
 */
data class IncidentPresentation(
    val incident: SessionIncidentDto,
    val action: ReadinessAction? = null,
    val actionLabel: String? = null,
    val resolved: Boolean = false,
) {
    val code: String get() = incident.code

    val severity: IncidentSeverityDto get() = incident.severity

    companion object {
        fun of(
            incident: SessionIncidentDto,
            report: ReadinessReport? = null,
        ): IncidentPresentation {
            val resolved = report?.let { isResolved(incident.code, it) } ?: false
            val action = IncidentRemediation.actionFor(incident.code).takeUnless { resolved }
            val label = action?.let(ActiveSessionTexts::actionLabel)
            return IncidentPresentation(
                incident = incident,
                action = action.takeIf { label != null },
                actionLabel = label,
                resolved = resolved,
            )
        }

        private fun isResolved(
            code: String,
            report: ReadinessReport,
        ): Boolean {
            val checkId = IncidentReadinessChecks.checkFor(code) ?: return false
            return report.checks.firstOrNull { it.id == checkId }?.outcome == ReadinessOutcome.PASSED
        }
    }
}
