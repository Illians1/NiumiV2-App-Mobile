package com.niumi.feature.setup.apps

import com.niumi.core.domain.AppSelectionSummary
import com.niumi.system.apps.InstalledApp

/**
 * Une ligne du sélecteur. [showPackageName] n'est vrai que si une autre application porte le même
 * libellé : SPEC_ANDROID §12.1 impose alors d'afficher le nom de package en petit texte, pour que
 * l'utilisateur puisse les distinguer.
 */
data class AppPickerItem(
    val app: InstalledApp,
    val isSelected: Boolean,
    val showPackageName: Boolean,
)

/**
 * État de l'écran 4. [canConfirm] applique les bornes de `:shared:core` sans les réécrire : la
 * règle appartient au moteur commun, l'écran ne fait qu'en refléter l'état (§12.1, dernier
 * alinéa) ; `evaluateActivation` tranche à l'activation.
 */
data class AppPickerUiState(
    val items: List<AppPickerItem> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val isSessionInProgress: Boolean = false,
) {
    val selectedCount: Int get() = items.count { it.isSelected }

    val canConfirm: Boolean
        get() =
            !isSessionInProgress &&
                selectedCount in AppSelectionSummary.MIN_COUNT..AppSelectionSummary.MAX_COUNT

    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}
