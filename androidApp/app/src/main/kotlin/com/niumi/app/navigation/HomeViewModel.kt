package com.niumi.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.system.session.SessionSnapshotPublisher
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
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val setupPreferences: SetupPreferences,
        private val snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        private var onboardingAcknowledged = false

        var state by mutableStateOf(HomeUiState())
            private set

        init {
            viewModelScope.launch {
                snapshotPublisher.snapshot.collectLatest { recompute() }
            }
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                onboardingAcknowledged = setupPreferences.isOnboardingAcknowledged()
                recompute()
            }
        }

        private fun recompute() {
            val destination = homeDestinationFor(snapshotPublisher.snapshot.value?.state, onboardingAcknowledged)
            state =
                HomeUiState(
                    destination = destination,
                    hasActiveSession = destination == NiumiRoute.ActiveSession,
                )
        }
    }
