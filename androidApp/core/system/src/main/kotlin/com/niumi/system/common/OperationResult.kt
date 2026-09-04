package com.niumi.system.common

/**
 * Résultat typé d'un adaptateur système. Tous les adaptateurs sont idempotents et renvoient
 * un résultat typé, jamais une exception (« Interfaces transverses » du plan MVP).
 */
sealed interface OperationResult {
    data object Success : OperationResult

    // Précondition déjà atteinte (SPEC_CORE_KMP §6, règle d'échappement).
    data object AlreadySatisfied : OperationResult

    data class Failure(
        val code: String,
        val cause: Throwable? = null,
    ) : OperationResult
}
