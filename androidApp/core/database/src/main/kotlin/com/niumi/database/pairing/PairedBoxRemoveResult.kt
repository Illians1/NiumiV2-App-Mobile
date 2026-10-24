package com.niumi.database.pairing

/**
 * Résultat typé de [PairedBoxStore.remove] (décision Q22 du 2026-10-19, SPEC_CORE_KMP §2 décision 16) :
 * il reste toujours au moins un Niumi Spot. Le refus du dernier se décide au dépôt, comme celui du
 * onzième à [PairedBoxStore.add] ; l'écran ne fait qu'afficher le résultat.
 */
sealed interface PairedBoxRemoveResult {
    data object Removed : PairedBoxRemoveResult

    /** Le Niumi Spot visé est le seul enregistré : rien n'est écrit. */
    data object LastBoxKept : PairedBoxRemoveResult

    /** Aucun Niumi Spot ne porte cet identifiant : rien n'est écrit. */
    data object Unknown : PairedBoxRemoveResult
}
