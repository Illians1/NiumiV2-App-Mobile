package com.niumi.feature.session.active

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.BlockedPackage
import com.niumi.feature.session.ui.WakeScheduleDisplay

/**
 * État de l'écran 7 (SPEC_ANDROID §15, écran 7), complet depuis l'étape 15 : état, date, heure et
 * fuseau d'activation, heure recalculée si le fuseau courant a changé (§8), applications bloquées,
 * santé et incidents.
 *
 * [displayInCurrentZone] n'est non nul que si le fuseau courant diffère de celui de l'activation :
 * l'instant ne change jamais après `ACTIVATION_SUCCEEDED` (§8), seule sa lecture locale change.
 *
 * [blockedApps] porte le `displayNameSnapshot` figé à l'activation, jamais un nom résolu au moment
 * de l'affichage : une application désinstallée ou renommée pendant la session doit rester
 * nommable (§12.2, même raison que l'écart de l'étape 5 sur `BlockingController.apply`).
 */
data class ActiveSessionUiState(
    val state: SessionStateDto? = null,
    val displayAtActivation: WakeScheduleDisplay? = null,
    val displayInCurrentZone: WakeScheduleDisplay? = null,
    val blockedApps: List<BlockedPackage> = emptyList(),
    val health: SessionHealthDto? = null,
    val incidents: List<SessionIncidentDto> = emptyList(),
    val isLoading: Boolean = true,
) {
    val hasSession: Boolean get() = state != null

    val isDegraded: Boolean get() = health == SessionHealthDto.DEGRADED

    /**
     * SPEC_CORE_KMP §7.3 : `CRITICAL` « doit en plus être présenté explicitement dans un
     * diagnostic visible par l'utilisateur », ce qui distingue sa présentation d'un `DEGRADED`
     * simplement consigné.
     */
    val criticalIncidents: List<SessionIncidentDto>
        get() = incidents.filter { it.severity == IncidentSeverityDto.CRITICAL }
}
