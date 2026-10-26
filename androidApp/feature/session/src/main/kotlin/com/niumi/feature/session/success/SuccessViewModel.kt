package com.niumi.feature.session.success

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.ReleaseMeansDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.TimeToGetUpInputDto
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.insight.TimeToGetUpPreferences
import com.niumi.system.session.SessionHistoryReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

sealed interface SuccessUiState {
    data object Loading : SuccessUiState

    /** Session introuvable ou pas terminée : l'écran rend la main à l'accueil. */
    data object NotFound : SuccessUiState

    /**
     * [afterAlarm] est nul quand l'alarme n'a pas sonné ; [savedComparison], la suite de
     * « Bravo ! », quand rien n'est à féliciter.
     */
    data class Shown(
        val title: String,
        val afterAlarm: String?,
        val savedComparison: String?,
    ) : SuccessUiState
}

/**
 * Écran 11 · Réussite (`design-plan/SPECIFICATION.md` §15, étape 46). Relit la session terminée par
 * son identifiant ([SessionHistoryReader]) : la session suivante est déjà armée quand l'écran
 * s'ouvre. Il n'écrit rien.
 *
 * - « Debout à » : l'heure du scan ou de la recopie (`releasingAtEpochMillis`), à l'heure locale.
 * - Délai : depuis la sonnerie réelle (`ringingAtEpochMillis`), calculé par le moteur commun
 *   (`computeTimeToGetUp`, minute inférieure) ; absent si l'alarme n'a pas sonné (téléphone éteint
 *   à l'heure), décision de Mehdi du 2026-10-25.
 * - « Bravo ! » : seulement après un scan (pas la recopie, D2), avec une durée déclarée à l'écran
 *   3 bis (étape 50) et un lever plus rapide qu'elle.
 */
@HiltViewModel
class SuccessViewModel
    @Inject
    constructor(
        private val historyReader: SessionHistoryReader,
        private val timeToGetUp: TimeToGetUpPreferences,
        private val facade: NiumiCoreFacade,
        private val timeZoneProvider: TimeZoneProvider,
    ) : ViewModel() {
        var state by mutableStateOf<SuccessUiState>(SuccessUiState.Loading)
            private set

        private var loadedSessionId: String? = null

        /** Idempotent pour une même session (recomposition, rotation). */
        fun load(sessionId: String) {
            if (loadedSessionId == sessionId) return
            loadedSessionId = sessionId
            viewModelScope.launch {
                val session = historyReader.find(sessionId)?.takeIf { it.state == SessionStateDto.COMPLETED }
                state = session?.let { shownFor(it) } ?: SuccessUiState.NotFound
            }
        }

        private suspend fun shownFor(session: SessionSnapshotDto): SuccessUiState? =
            session.releasingAtEpochMillis?.let { releasedAt ->
                val zone = ZoneId.of(timeZoneProvider.currentZoneId())
                val title = SuccessTexts.standingAt(Instant.ofEpochMilli(releasedAt).atZone(zone).toLocalTime())
                val ringingAt = session.ringingAtEpochMillis
                if (ringingAt == null) {
                    SuccessUiState.Shown(title, afterAlarm = null, savedComparison = null)
                } else {
                    shownAfterRinging(session, title, ringingAt, releasedAt)
                }
            }

        private suspend fun shownAfterRinging(
            session: SessionSnapshotDto,
            title: String,
            ringingAt: Long,
            releasedAt: Long,
        ): SuccessUiState.Shown {
            val declared = timeToGetUp.declaredMinutes()
            val result =
                facade.computeTimeToGetUp(
                    TimeToGetUpInputDto(
                        alarmAtEpochMillis = ringingAt,
                        releasedAtEpochMillis = releasedAt,
                        declaredMinutes = declared ?: 0,
                    ),
                )
            val saved =
                result.savedMinutes?.takeIf { declared != null && session.releasedBy == ReleaseMeansDto.NFC_SCAN }
            return SuccessUiState.Shown(
                title = title,
                afterAlarm = SuccessTexts.afterAlarm(result.elapsedMinutes),
                savedComparison = saved?.let(SuccessTexts::savedComparison),
            )
        }
    }
