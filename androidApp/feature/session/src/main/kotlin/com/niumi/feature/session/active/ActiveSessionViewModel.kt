package com.niumi.feature.session.active

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.BlockedPackage
import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.feature.session.ui.WakeScheduleFormatter
import com.niumi.system.common.Clock
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.readiness.ForegroundReadinessTrigger
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Écran 7 complet (SPEC_ANDROID §15). Projette le snapshot publié : l'instant de déclenchement est
 * immuable après `ACTIVATION_SUCCEEDED` (§8), seule sa lecture locale peut changer avec le fuseau
 * du téléphone — d'où deux projections quand les deux fuseaux diffèrent, jamais un recalcul de la
 * session.
 *
 * Deux lectures s'ajoutent au snapshot, qui ne les porte pas : les applications bloquées viennent
 * d'`AndroidSessionExtras` via [gateway] (unlock-aware, donc jamais un accès Room brut), les
 * incidents d'[incidentsReader]. Elles sont relues à chaque décision publiée : un incident est
 * précisément ce qui arrive **pendant** une session.
 */
@HiltViewModel
class ActiveSessionViewModel
    @Inject
    constructor(
        private val clock: Clock,
        private val timeZoneProvider: TimeZoneProvider,
        private val snapshotPublisher: SessionSnapshotPublisher,
        private val gateway: SessionPersistenceGateway,
        private val incidentsReader: SessionIncidentsReader,
        private val readinessTrigger: ForegroundReadinessTrigger,
    ) : ViewModel() {
        var state by mutableStateOf(ActiveSessionUiState())
            private set

        /**
         * Convention 12/24 h du système, fournie par la `Route` comme sur les écrans 5 et 6 :
         * trois écrans portent la même heure, ils ne peuvent pas employer deux conventions (§15).
         */
        private var use24Hour = true

        init {
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot -> state = project(snapshot, loadDetails(snapshot)) }
            }
        }

        /**
         * Appelé à chaque `ON_RESUME`. Relit la convention horaire, les détails persistés, et
         * déclenche la surveillance de §13.1 : « passage de l'application au premier plan » y est
         * un déclencheur, qui n'avait jusqu'ici aucun appelant. C'est ce qui rend visible un
         * service d'accessibilité désactivé pendant que Niumi était en arrière-plan.
         */
        fun refresh(use24Hour: Boolean) {
            this.use24Hour = use24Hour
            readinessTrigger.evaluateAsync()
            viewModelScope.launch {
                val snapshot = snapshotPublisher.snapshot.value
                state = project(snapshot, loadDetails(snapshot))
            }
        }

        private suspend fun loadDetails(snapshot: SessionSnapshotDto?): SessionDetails {
            if (snapshot == null) return SessionDetails()
            val blockedApps =
                when (val loaded = gateway.load()) {
                    // Un snapshot illisible ne doit pas se présenter comme une session sans
                    // application bloquée (§13) : on n'affirme rien plutôt que d'affirmer « aucune ».
                    is LoadResult.Present -> loaded.extras.blockedPackages

                    LoadResult.Absent, is LoadResult.Unreadable -> null
                }
            return SessionDetails(
                blockedApps = blockedApps,
                incidents = incidentsReader.incidents(snapshot.sessionId).sortedWith(INCIDENT_ORDER),
            )
        }

        private fun project(
            snapshot: SessionSnapshotDto?,
            details: SessionDetails,
        ): ActiveSessionUiState {
            if (snapshot == null) return ActiveSessionUiState(isLoading = false)
            val nowEpochMillis = clock.nowEpochMillis()
            val currentZoneId = timeZoneProvider.currentZoneId()
            val schedule = snapshot.wakeSchedule
            return ActiveSessionUiState(
                state = snapshot.state,
                displayAtActivation = WakeScheduleFormatter.format(schedule, nowEpochMillis, use24Hour = use24Hour),
                displayInCurrentZone =
                    currentZoneId
                        .takeIf { it != schedule.zoneIdAtActivation }
                        ?.let {
                            WakeScheduleFormatter.format(
                                schedule,
                                nowEpochMillis,
                                displayZoneId = it,
                                use24Hour = use24Hour,
                            )
                        },
                blockedApps = details.blockedApps ?: state.blockedApps,
                health = snapshot.health,
                incidents = details.incidents,
                isLoading = false,
            )
        }

        /**
         * [blockedApps] nul signifie « la persistance n'a rien pu dire », distinct d'une liste
         * vide qui signifie « aucune application sélectionnée » : la projection conserve alors ce
         * qu'elle affichait.
         */
        private data class SessionDetails(
            val blockedApps: List<BlockedPackage>? = null,
            val incidents: List<SessionIncidentDto> = emptyList(),
        )

        private companion object {
            /**
             * `CRITICAL` d'abord, puis `DEGRADED`, puis `WARNING` (SPEC_CORE_KMP §7.3), et à
             * gravité égale le plus récent en premier.
             */
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
