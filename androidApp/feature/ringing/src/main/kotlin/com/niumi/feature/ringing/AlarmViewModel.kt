package com.niumi.feature.ringing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.PendingEffect
import com.niumi.feature.ringing.ui.AlarmUiState
import com.niumi.feature.ringing.ui.ReleaseProgress
import com.niumi.feature.ringing.ui.ReleaseStep
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * État de l'écran de réveil (SPEC_ANDROID §10.4). Lit la persistance **puis** suit le publisher,
 * jamais l'inverse : `SessionSnapshotPublisher` vit en mémoire et vaut `null` dans tout processus
 * neuf — c'est toujours le cas quand le plein écran ouvre l'activité après un réveil. Collecter le
 * flux brut fermerait l'écran avant même que le coordinateur ait publié quoi que ce soit, privant
 * l'utilisateur du seul accès au scan.
 *
 * N'écrit jamais : ni `dispatch`, ni `commit`. L'écran de réveil observe, le scan décide
 * (SPEC_ANDROID §3, §11.3) — d'où l'absence de `SessionCoordinator` ici.
 *
 * Les entrées locales à l'appareil (verrouillage, disponibilité NFC, dernier résultat de scan)
 * sont poussées par l'activité : elles n'ont pas de source observable côté domaine, et les garder
 * ici évite à l'activité de recomposer elle-même un état déjà décrit par [AlarmUiState].
 */
@HiltViewModel
class AlarmViewModel
    @Inject
    constructor(
        private val gateway: SessionPersistenceGateway,
        private val snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        private var sessionState: SessionStateDto? = null
        private var releaseSteps: List<ReleaseStep> = emptyList()
        private var sessionConfirmedGone = false
        private var deviceLocked = false
        private var nfcAvailability = NfcAvailability.ENABLED
        private var lastScanOutcome: ScanOutcome? = null

        var state: AlarmUiState by mutableStateOf(AlarmUiState.Loading)
            private set

        init {
            viewModelScope.launch {
                adoptLoaded(gateway.load())
                snapshotPublisher.snapshot.filterNotNull().collect { adoptSnapshot(it, pendingEffects = null) }
            }
        }

        fun onDeviceLockChanged(locked: Boolean) {
            deviceLocked = locked
            recompute()
        }

        fun onNfcAvailabilityChanged(availability: NfcAvailability) {
            nfcAvailability = availability
            recompute()
        }

        fun onScanOutcome(outcome: ScanOutcome?) {
            lastScanOutcome = outcome
            recompute()
        }

        private suspend fun adoptLoaded(loaded: LoadResult) {
            when (loaded) {
                is LoadResult.Present -> adoptSnapshot(loaded.snapshot, loaded.pendingEffects)

                // Seule une absence confirmée par la persistance ferme l'écran.
                LoadResult.Absent -> confirmSessionGone()

                // SPEC_CORE_KMP §13 : un snapshot illisible ne se lit jamais « pas de session ».
                // L'écran garde ce qu'il sait, et la réconciliation tranchera.
                is LoadResult.Unreadable -> Unit
            }
        }

        private fun confirmSessionGone() {
            sessionConfirmedGone = true
            recompute()
        }

        private suspend fun adoptSnapshot(
            snapshot: SessionSnapshotDto,
            pendingEffects: List<PendingEffect>?,
        ) {
            sessionConfirmedGone = false
            sessionState = snapshot.state
            releaseSteps =
                if (snapshot.state == SessionStateDto.RELEASING) {
                    ReleaseProgress.from(pendingEffects ?: gateway.pendingEffects(snapshot.sessionId))
                } else {
                    emptyList()
                }
            recompute()
        }

        private fun recompute() {
            val current = sessionState
            state =
                when {
                    sessionConfirmedGone -> {
                        AlarmUiState.Close
                    }

                    current == null -> {
                        AlarmUiState.Loading
                    }

                    else -> {
                        AlarmUiState.forSession(
                            sessionState = current,
                            deviceLocked = deviceLocked,
                            nfcAvailability = nfcAvailability,
                            lastScanOutcome = lastScanOutcome,
                            releaseSteps = releaseSteps,
                        )
                    }
                }
        }
    }
