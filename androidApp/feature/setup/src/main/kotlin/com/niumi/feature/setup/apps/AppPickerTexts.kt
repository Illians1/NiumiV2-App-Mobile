package com.niumi.feature.setup.apps

import com.niumi.core.domain.AppSelectionSummary

/**
 * Textes de l'écran 4 (SPEC_ANDROID §15, §12.1). Les bornes affichées viennent de
 * `AppSelectionSummary` (`:shared:core`) : la règle 1..50 appartient au moteur commun, l'écran ne
 * fait que la refléter (SPEC_CORE_KMP §2 point 12).
 */
object AppPickerTexts {
    const val TITLE = "Choisis les applications à bloquer"

    const val INSTRUCTION =
        "Elles resteront inaccessibles jusqu'au scan de ton boîtier, réveil compris."

    const val LOADING = "Recherche des applications installées…"

    const val EMPTY = "Aucune application à proposer sur cet appareil."

    const val CONFIRM_BUTTON_LABEL = "Valider ma sélection"

    const val SESSION_IN_PROGRESS =
        "Une session est en cours. Ta sélection ne peut pas être modifiée avant sa fin."

    val TOO_MANY_MESSAGE =
        "Tu as atteint la limite de ${AppSelectionSummary.MAX_COUNT} applications. " +
            "Retires-en une pour en ajouter une autre."

    val NONE_SELECTED_MESSAGE =
        "Sélectionne au moins ${AppSelectionSummary.MIN_COUNT} application pour continuer."

    fun counterLabel(selectedCount: Int): String = "$selectedCount / ${AppSelectionSummary.MAX_COUNT}"
}
