package com.niumi.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.StorageIntegrityState
import com.niumi.system.setup.SetupCompletion
import com.niumi.system.setup.SetupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Où mène l'ouverture de Niumi (étape 45) : [destination] vaut `null` tant que les préférences n'ont
 * pas été lues, puis la route calculée par [homeDestinationFor]. Relue à chaque `ON_RESUME` : la
 * présentation ou la mise en route peuvent s'achever pendant la vie de l'écran.
 */
@HiltViewModel
class HomeGateViewModel
    @Inject
    constructor(
        private val setupPreferences: SetupPreferences,
        private val setupCompletion: SetupCompletion,
        private val snapshotPublisher: SessionSnapshotPublisher,
        private val storageIntegrity: StorageIntegrityState,
    ) : ViewModel() {
        var destination by mutableStateOf<NiumiRoute?>(null)
            private set

        private var acknowledged: Boolean? = null
        private var completed: Boolean? = null

        init {
            viewModelScope.launch { snapshotPublisher.snapshot.collectLatest { recompute() } }
            viewModelScope.launch { storageIntegrity.failure.collectLatest { recompute() } }
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                acknowledged = setupPreferences.isOnboardingAcknowledged()
                completed = setupCompletion.isCompleted()
                recompute()
            }
        }

        private fun recompute() {
            val acknowledged = acknowledged ?: return
            val completed = completed ?: return
            destination =
                homeDestinationFor(
                    state = snapshotPublisher.snapshot.value?.state,
                    onboardingAcknowledged = acknowledged,
                    setupCompleted = completed,
                    storageUnreadable = storageIntegrity.failure.value != null,
                )
        }
    }
