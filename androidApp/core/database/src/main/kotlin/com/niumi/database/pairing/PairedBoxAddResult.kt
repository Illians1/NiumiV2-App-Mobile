package com.niumi.database.pairing

/**
 * Résultat typé de [PairedBoxStore.add] (SPEC_ANDROID §11.1, Lot 8) : le refus d'un doublon ou
 * d'un sixième boîtier se décide au dépôt, l'écran ne fait qu'afficher le résultat.
 */
sealed interface PairedBoxAddResult {
    data object Added : PairedBoxAddResult

    data object AlreadyPaired : PairedBoxAddResult

    data object LimitReached : PairedBoxAddResult
}
