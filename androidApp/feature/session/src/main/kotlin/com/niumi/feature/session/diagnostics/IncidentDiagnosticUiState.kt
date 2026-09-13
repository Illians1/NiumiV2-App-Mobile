package com.niumi.feature.session.diagnostics

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.system.readiness.ReadinessCheck

/**
 * État de l'écran 12 — diagnostic d'incident (SPEC_ANDROID §15, §17, §18).
 *
 * [incidents] est trié `CRITICAL` d'abord (SPEC_CORE_KMP §7.3 : un `CRITICAL` « doit en plus être
 * présenté explicitement dans un diagnostic visible par l'utilisateur »), puis du plus récent au
 * plus ancien.
 *
 * L'écran est consultable **sans session active** : le journal technique et les contrôles de §13
 * restent utiles quand la session vient de se terminer ou d'échouer.
 */
data class IncidentDiagnosticUiState(
    val sessionId: String? = null,
    val state: SessionStateDto? = null,
    val health: SessionHealthDto? = null,
    val checks: List<ReadinessCheck> = emptyList(),
    val incidents: List<SessionIncidentDto> = emptyList(),
    val events: List<TechnicalEventEntry> = emptyList(),
    val isLoading: Boolean = true,
) {
    val hasSession: Boolean get() = sessionId != null

    val criticalIncidents: List<SessionIncidentDto>
        get() = incidents.filter { it.severity == IncidentSeverityDto.CRITICAL }
}
