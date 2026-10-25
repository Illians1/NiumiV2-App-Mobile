package com.niumi.feature.setup.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.database.BlockedPackage
import com.niumi.feature.setup.isAppSelectionEditable
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.apps.InstalledApp
import com.niumi.system.apps.InstalledAppsSource
import com.niumi.system.plan.PlanChangeReason
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

/**
 * Écran 4 · Choix des applications (`SPECIFICATION.md` §8, SPEC_ANDROID §12.1).
 *
 * La page travaille sur un brouillon distinct de la sélection enregistrée : la croix l'abandonne,
 * seul « Valider » écrit `AppSelectionStore.replace()` (une fois, garde contre le double tap), puis
 * la page se ferme. La carte montre la sélection validée (Q3, 2026-10-22). Page ouverte, brouillon,
 * section « Applications déjà choisies » et recherche vivent dans le [SavedStateHandle] : ils survivent à la rotation
 * comme à une mort du processus.
 *
 * Les bornes 1..50 viennent d'`AppSelectionSummary` (`:shared:core`) : l'écran empêche seulement
 * d'atteindre un état que `evaluateActivation` refuserait. Le libellé est figé dans
 * [BlockedPackage] à la validation (§12.2) : une application désinstallée plus tard garde un nom.
 */
@HiltViewModel
class AppSelectionViewModel
    @Inject
    constructor(
        private val installedAppsSource: InstalledAppsSource,
        private val appSelectionStore: AppSelectionStore,
        snapshotPublisher: SessionSnapshotPublisher,
        private val planning: AppSelectionPlanning,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        var state by mutableStateOf(AppSelectionUiState())
            private set

        private var apps: List<InstalledApp> = emptyList()
        private var savedPackages: Set<String> = emptySet()
        private var duplicatedLabels: Set<String> = emptySet()
        private var validating = false
        private var justSaved = false

        init {
            viewModelScope.launch { load() }
            // Décision 11 amendée le 2026-10-24 (étape 45) : la sélection se modifie jusqu'au coucher,
            // puis reste verrouillée jusqu'au scan ; la route se retire alors de la pile.
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot ->
                    onSessionStateChanged(!isAppSelectionEditable(snapshot))
                }
            }
        }

        fun openPage() {
            if (state.isSessionInProgress) return
            justSaved = false
            savedStateHandle[DRAFT_KEY] = ArrayList(savedPackages)
            savedStateHandle[PINNED_KEY] = ArrayList(savedPackages)
            savedStateHandle[QUERY_KEY] = ""
            refresh()
        }

        fun toggle(packageName: String) {
            val draft = draft() ?: return
            val updated =
                when {
                    packageName in draft -> draft - packageName
                    draft.size >= AppSelectionSummary.MAX_COUNT -> return
                    else -> draft + packageName
                }
            savedStateHandle[DRAFT_KEY] = ArrayList(updated)
            refresh()
        }

        fun search(query: String) {
            if (draft() == null) return
            savedStateHandle[QUERY_KEY] = query
            refresh()
        }

        /** La croix ou le retour : le brouillon est abandonné. */
        fun dismissPage() {
            justSaved = false
            savedStateHandle.remove<ArrayList<String>>(DRAFT_KEY)
            savedStateHandle.remove<ArrayList<String>>(PINNED_KEY)
            savedStateHandle.remove<String>(QUERY_KEY)
            refresh()
        }

        fun validate() {
            val page = state.page
            val chosen = apps.filter { page != null && it.packageName in page.draft }
            // Ce qui est écrit, pas seulement le brouillon, doit tenir dans les bornes communes.
            val writable =
                page?.canValidate == true &&
                    chosen.size in AppSelectionSummary.MIN_COUNT..AppSelectionSummary.MAX_COUNT
            if (!writable || validating) return
            validating = true
            refresh()
            viewModelScope.launch {
                try {
                    appSelectionStore.replace(chosen.map { BlockedPackage(it.packageName, it.label) })
                    savedPackages = chosen.mapTo(mutableSetOf()) { it.packageName }
                    planning.rearmAfterSelection()
                    // La page se ferme d'elle-même en voyant `isSaved`, puis appelle [dismissPage].
                    justSaved = true
                } finally {
                    validating = false
                    refresh()
                }
            }
        }

        /**
         * « Continuer » de la mise en route : l'accueil s'ouvrira désormais directement (étape 45,
         * `design-plan/SPECIFICATION.md` §3). [onDone] est appelé même si l'écriture échoue : la
         * mise en route reste déductible des Niumi Spots et des applications (`SetupCompletion`).
         */
        fun completeSetup(onDone: () -> Unit) {
            viewModelScope.launch {
                try {
                    planning.setupPreferences.markSetupCompleted()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (
                    @Suppress("TooGenericExceptionCaught") _: Exception,
                ) {
                    // La mise en route reste déductible des Niumi Spots et des applications.
                }
                onDone()
            }
        }

        fun onSessionStateChanged(isSessionInProgress: Boolean) {
            state = state.copy(isSessionInProgress = isSessionInProgress)
            refresh()
        }

        private suspend fun load() {
            apps = installedAppsSource.launchableApps()
            val installed = apps.mapTo(mutableSetOf()) { it.packageName }
            // Une application enregistrée puis désinstallée disparaît : la montrer serait un faux
            // état (SPEC_ANDROID §15).
            savedPackages =
                appSelectionStore
                    .selection()
                    .map { it.packageName }
                    .filterTo(mutableSetOf()) { it in installed }
            duplicatedLabels =
                apps
                    .groupingBy { it.label }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
            draft()?.let { restored -> savedStateHandle[DRAFT_KEY] = ArrayList(restored.filter { it in installed }) }
            state = state.copy(isLoading = false)
            refresh()
        }

        private fun refresh() {
            state =
                state.copy(
                    saved = apps.filter { it.packageName in savedPackages },
                    page = draft()?.let { draft -> pageState(draft, savedStateHandle[QUERY_KEY] ?: "") },
                )
        }

        private fun pageState(
            draft: Set<String>,
            query: String,
        ): AppSelectionPageState {
            val atLimit = draft.size >= AppSelectionSummary.MAX_COUNT
            val needle = query.searchKey()
            val pinned = savedStateHandle.get<ArrayList<String>>(PINNED_KEY)?.toSet().orEmpty()
            val (chosen, others) =
                apps
                    .filter { needle.isEmpty() || it.label.searchKey().contains(needle) }
                    .map { app ->
                        val selected = app.packageName in draft
                        AppSelectionItem(
                            app = app,
                            isSelected = selected,
                            isEnabled = selected || !atLimit,
                            showPackageName = app.label in duplicatedLabels,
                        )
                    }.partition { it.app.packageName in pinned }
            return AppSelectionPageState(
                draft = draft,
                query = query,
                items = others,
                chosenItems = chosen,
                isLoading = state.isLoading,
                hasApps = apps.isNotEmpty(),
                isSessionInProgress = state.isSessionInProgress,
                isSaving = validating,
                isSaved = justSaved,
            )
        }

        private fun draft(): Set<String>? = savedStateHandle.get<ArrayList<String>>(DRAFT_KEY)?.toSet()

        private companion object {
            const val DRAFT_KEY = "draft"
            const val QUERY_KEY = "query"
            const val PINNED_KEY = "pinned"

            private val DIACRITICS = Regex("\\p{Mn}+")

            /** Recherche insensible à la casse et aux accents : « telephone » trouve « Téléphone ». */
            fun String.searchKey(): String =
                Normalizer
                    .normalize(trim(), Normalizer.Form.NFD)
                    .replace(DIACRITICS, "")
                    .lowercase()
        }
    }
