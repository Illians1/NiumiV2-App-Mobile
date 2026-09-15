package com.niumi.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.StorageIntegrityState
import com.niumi.system.session.isSessionInProgress
import com.niumi.system.setup.SetupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * L'accusé de réception de l'onboarding est relu à chaque `ON_RESUME` : il change pendant la vie
 * de l'écran, l'utilisateur pouvant revenir de l'onboarding. Le snapshot de session, lui, est un
 * `StateFlow` collecté en continu — une session peut être armée ou libérée par une réconciliation
 * pendant que l'accueil est affiché.
 *
 * Étape 17 : tant que la session attend un scan, l'accueil redirige vers l'écran 8 (§10.4) — et
 * le fait à **chaque** passage au premier plan, [refresh] réarmant la redirection. Aucun autre
 * écran de Niumi n'est donc atteignable pendant la sonnerie. [onAlarmScreenOpened] ne sert qu'à
 * éviter un second lancement dans le même passage au premier plan ; ce n'est pas un renoncement.
 *
 * Il n'y a pas de boucle : l'écran de réveil renvoie au **lanceur** quand on le quitte, pas à
 * l'accueil, donc l'accueil n'est jamais repris tant que la session sonne.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val setupPreferences: SetupPreferences,
        private val snapshotPublisher: SessionSnapshotPublisher,
        private val storageIntegrity: StorageIntegrityState,
    ) : ViewModel() {
        private var onboardingAcknowledged = false
        private var alarmScreenAcknowledged = false

        var state by mutableStateOf(HomeUiState())
            private set

        init {
            viewModelScope.launch {
                snapshotPublisher.snapshot.collectLatest { recompute() }
            }
            viewModelScope.launch {
                storageIntegrity.failure.collectLatest { recompute() }
            }
            refresh()
        }

        fun onAlarmScreenOpened() {
            alarmScreenAcknowledged = true
            recompute()
        }

        /** Rejoué à chaque `ON_RESUME` : réarme aussi la redirection vers l'écran 8. */
        fun refresh() {
            alarmScreenAcknowledged = false
            viewModelScope.launch {
                onboardingAcknowledged = setupPreferences.isOnboardingAcknowledged()
                recompute()
            }
        }

        private fun recompute() {
            val sessionState = snapshotPublisher.snapshot.value?.state
            val storageUnreadable = storageIntegrity.failure.value != null
            val launcher = launcherDestinationFor(sessionState, onboardingAcknowledged)
            state =
                HomeUiState(
                    destination = homeDestinationFor(sessionState, onboardingAcknowledged, storageUnreadable),
                    hasActiveSession = sessionState.isSessionInProgress(),
                    alarmScreenRequired = launcher == LauncherDestination.AlarmScreen,
                    pendingAlarmScreen =
                        launcher == LauncherDestination.AlarmScreen && !alarmScreenAcknowledged,
                    storageUnreadable = storageUnreadable,
                )
        }
    }
