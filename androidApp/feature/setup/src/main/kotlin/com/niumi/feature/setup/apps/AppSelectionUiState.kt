package com.niumi.feature.setup.apps

import com.niumi.core.domain.AppSelectionSummary
import com.niumi.system.apps.InstalledApp

/**
 * Une case de la grille de la page. [isEnabled] est faux pour une application non cochée quand
 * la limite commune est atteinte (décision du 2026-10-22). [showPackageName] n'est vrai que si une
 * autre application porte le même libellé (SPEC_ANDROID §12.1).
 */
data class AppSelectionItem(
    val app: InstalledApp,
    val isSelected: Boolean,
    val isEnabled: Boolean,
    val showPackageName: Boolean,
)

/**
 * Feuille « Applications à bloquer » ouverte. [draft] est le brouillon : rien n'est écrit avant
 * « Valider », la croix l'abandonne (`SPECIFICATION.md` §8). [items] est la grille filtrée par
 * [query] ; une application cochée masquée par la recherche reste dans [draft] et compte.
 */
data class AppSelectionPageState(
    val draft: Set<String>,
    val query: String,
    val items: List<AppSelectionItem>,
    /**
     * Section « Applications déjà choisies », en tête : les applications cochées à l'ouverture de la page, filtrées
     * par la recherche ; [items] ne les répète pas. Fixée pour la visite, pour que rien ne saute
     * sous le doigt (décision de Mehdi du 2026-10-22).
     */
    val chosenItems: List<AppSelectionItem>,
    val isLoading: Boolean,
    val hasApps: Boolean,
    val isSessionInProgress: Boolean,
    /** Écriture en cours après « Valider ». */
    val isSaving: Boolean = false,
    /** Sélection enregistrée : la page se ferme. */
    val isSaved: Boolean = false,
) {
    val isAtLimit: Boolean get() = draft.size >= AppSelectionSummary.MAX_COUNT

    /**
     * Bornes 1..50 de `:shared:core`, jamais réécrites ici. Jamais pendant le chargement : un
     * brouillon restauré après une mort du processus précède la liste des applications, et le
     * valider écrirait une sélection vide (relecture du 2026-10-22).
     */
    val canValidate: Boolean
        get() =
            !isLoading &&
                !isSessionInProgress &&
                !isSaving &&
                !isSaved &&
                draft.size in AppSelectionSummary.MIN_COUNT..AppSelectionSummary.MAX_COUNT

    val counter: String
        get() = if (isAtLimit) AppSelectionTexts.limitReached() else AppSelectionTexts.chosen(draft.size)

    /** Texte d'une grille vide ; aucun pendant le chargement (décision du 2026-10-22). */
    val emptyMessage: String?
        get() =
            when {
                isLoading -> null
                !hasApps -> AppSelectionTexts.NO_APPS
                items.isEmpty() && chosenItems.isEmpty() -> AppSelectionTexts.NO_MATCH
                else -> null
            }
}

/**
 * État de l'écran 4. [saved] est la sélection **validée** (Q3, 2026-10-22) : la carte ne montre
 * qu'elle, jamais le brouillon de la page.
 */
data class AppSelectionUiState(
    val saved: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
    val isSessionInProgress: Boolean = false,
    val page: AppSelectionPageState? = null,
) {
    val canContinue: Boolean get() = saved.isNotEmpty()
}
