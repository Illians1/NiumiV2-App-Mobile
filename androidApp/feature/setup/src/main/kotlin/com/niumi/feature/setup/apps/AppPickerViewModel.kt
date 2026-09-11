package com.niumi.feature.setup.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.database.BlockedPackage
import com.niumi.feature.setup.isSetupEditable
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.apps.InstalledApp
import com.niumi.system.apps.InstalledAppsSource
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sélecteur d'applications (écran 4, SPEC_ANDROID §12.1). Les bornes 1..50 viennent de
 * `AppSelectionSummary` (`:shared:core`) : cet écran ne redéfinit pas la règle, il empêche
 * seulement d'atteindre un état que `evaluateActivation` refuserait — une 51ᵉ coche n'est jamais
 * enregistrée, et la confirmation reste inactive à 0.
 *
 * Le libellé est figé dans [BlockedPackage] au moment de la validation (§12.2) : une application
 * désinstallée ou masquée plus tard garde un nom affichable pour l'overlay.
 */
@HiltViewModel
class AppPickerViewModel
    @Inject
    constructor(
        private val installedAppsSource: InstalledAppsSource,
        private val appSelectionStore: AppSelectionStore,
        snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        var state by mutableStateOf(AppPickerUiState())
            private set

        private var selectedPackages: Set<String> = emptySet()
        private var apps: List<InstalledApp> = emptyList()

        init {
            viewModelScope.launch { load() }
            // Garde de `SetupGate` : la sélection figée dans une session en cours ne peut pas
            // changer (SPEC_CORE_KMP §2 point 11).
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot ->
                    onSessionStateChanged(!isSetupEditable(snapshot?.state))
                }
            }
        }

        fun toggle(packageName: String) {
            val isSelected = packageName in selectedPackages
            if (!isSelected && selectedPackages.size >= AppSelectionSummary.MAX_COUNT) {
                state = state.copy(message = AppPickerTexts.TOO_MANY_MESSAGE)
                return
            }
            selectedPackages =
                if (isSelected) selectedPackages - packageName else selectedPackages + packageName
            state = state.copy(items = buildItems(), message = null)
        }

        fun onSessionStateChanged(isSessionInProgress: Boolean) {
            state =
                state.copy(
                    isSessionInProgress = isSessionInProgress,
                    message = if (isSessionInProgress) AppPickerTexts.SESSION_IN_PROGRESS else state.message,
                )
        }

        /** Ne persiste rien si la sélection sortirait des bornes communes. */
        fun confirm(onConfirmed: () -> Unit = {}) {
            if (!state.canConfirm) {
                state = state.copy(message = AppPickerTexts.NONE_SELECTED_MESSAGE)
                return
            }
            viewModelScope.launch {
                appSelectionStore.replace(
                    apps
                        .filter { it.packageName in selectedPackages }
                        .map { BlockedPackage(it.packageName, it.label) },
                )
                onConfirmed()
            }
        }

        private suspend fun load() {
            apps = installedAppsSource.launchableApps()
            val installedPackages = apps.mapTo(mutableSetOf()) { it.packageName }
            // Une application enregistrée puis désinstallée disparaît de la sélection : la
            // proposer à nouveau serait un faux état (§15).
            selectedPackages =
                appSelectionStore
                    .selection()
                    .map { it.packageName }
                    .filterTo(mutableSetOf()) { it in installedPackages }
            state = state.copy(items = buildItems(), isLoading = false)
        }

        private fun buildItems(): List<AppPickerItem> {
            val duplicatedLabels =
                apps
                    .groupingBy { it.label }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
            return apps.map { app ->
                AppPickerItem(
                    app = app,
                    isSelected = app.packageName in selectedPackages,
                    showPackageName = app.label in duplicatedLabels,
                )
            }
        }
    }
