package com.niumi.feature.session.diagnostics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.DeviceContext
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessInput
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/**
 * Écran 12 — diagnostic d'incident (SPEC_ANDROID §15, §17, §18).
 *
 * Rejoue [DeviceReadinessChecker] à chaque affichage plutôt que de lire un dernier résultat mis en
 * cache : le contrôle est explicitement sans état (§13.1, « il relit ses sources à chaque appel »),
 * et un diagnostic qui afficherait un état vieux de plusieurs minutes serait précisément le « faux
 * état de fiabilité » que §15 interdit. `candidateTriggerAtEpochMillis` reste nul : l'heure de
 * réveil est déjà figée, `FUTURE_TRIGGER` n'a rien à valider ici.
 *
 * [exportText] ne reçoit **jamais** `boxTokenSha256Hex` : seul `boxId` traverse, et l'exporteur le
 * tronque (§17).
 */
@HiltViewModel
class IncidentDiagnosticViewModel
    @Inject
    constructor(
        private val sources: DiagnosticSources,
        private val deviceContext: DeviceContext,
        private val timeZoneProvider: TimeZoneProvider,
    ) : ViewModel() {
        var state by mutableStateOf(IncidentDiagnosticUiState())
            private set

        private var boxId: String? = null

        /**
         * Filet de sécurité (étape 20), en plus des sources elles-mêmes déjà protégées
         * (`RoomTechnicalEventLog.recent()`, `RoomSessionIncidentsReader.incidents()`,
         * `UnlockAwarePersistenceGateway.load()`) : une exception inattendue ne doit jamais laisser
         * l'écran en `isLoading` indéfiniment.
         */
        fun refresh() {
            viewModelScope.launch {
                state =
                    runCatching { load(sources.snapshotPublisher.snapshot.value) }
                        .getOrElse {
                            IncidentDiagnosticUiState(
                                storageFailureReason =
                                    sources.storageIntegrity.failure.value ?: "DIAGNOSTIC_UNAVAILABLE",
                                isLoading = false,
                            )
                        }
            }
        }

        /**
         * §17 : « après action explicite de l'utilisateur ». Construit le texte à partir de l'état
         * déjà affiché — l'utilisateur exporte ce qu'il voit, pas un second diagnostic silencieux.
         */
        fun exportText(): String =
            DiagnosticExporter.export(
                DiagnosticReport(
                    deviceContext = deviceContext,
                    sessionId = state.sessionId,
                    state = state.state,
                    health = state.health,
                    boxId = boxId,
                    checks = state.checks,
                    incidents = state.incidents,
                    events = state.events,
                ),
                ZoneId.of(timeZoneProvider.currentZoneId()),
            )

        private suspend fun load(snapshot: SessionSnapshotDto?): IncidentDiagnosticUiState {
            val storageFailureReason = sources.storageIntegrity.failure.value
            val checks = sources.readinessChecker.check(ReadinessInput()).checks
            val events = sources.technicalEventLog.recent()
            if (snapshot == null) {
                boxId = null
                return IncidentDiagnosticUiState(
                    checks = checks,
                    events = events,
                    storageFailureReason = storageFailureReason,
                    isLoading = false,
                )
            }

            boxId = (sources.gateway.load() as? LoadResult.Present)?.extras?.boxId
            return IncidentDiagnosticUiState(
                sessionId = snapshot.sessionId,
                state = snapshot.state,
                health = snapshot.health,
                checks = checks,
                incidents = sources.incidentsReader.incidents(snapshot.sessionId).sortedWith(INCIDENT_ORDER),
                events = events,
                storageFailureReason = storageFailureReason,
                isLoading = false,
            )
        }

        private companion object {
            /** `CRITICAL`, puis `DEGRADED`, puis `WARNING` (SPEC_CORE_KMP §7.3) ; plus récent d'abord. */
            val INCIDENT_ORDER =
                compareByDescending<SessionIncidentDto> { severityRank(it.severity) }
                    .thenByDescending { it.occurredAtEpochMillis }

            fun severityRank(severity: IncidentSeverityDto): Int =
                when (severity) {
                    IncidentSeverityDto.CRITICAL -> 2
                    IncidentSeverityDto.DEGRADED -> 1
                    IncidentSeverityDto.WARNING -> 0
                }
        }
    }
