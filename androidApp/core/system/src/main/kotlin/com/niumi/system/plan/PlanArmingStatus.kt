package com.niumi.system.plan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Dernier armement tenté depuis le planning (décision Q14 du 2026-10-24). */
sealed interface PlanArmingState {
    /** Aucun échec en cours : armé, ou rien à armer (interrupteur éteint, aucun jour coché). */
    data object Ok : PlanArmingState

    /** Le planning est allumé mais rien n'a pu être armé : l'accueil ne doit pas montrer « Réveil prévu ». */
    data class Failed(
        val failure: ActivationFailure,
        val atEpochMillis: Long,
    ) : PlanArmingState
}

/**
 * État observable du dernier armement depuis le planning (décision Q14 du 2026-10-24). La fenêtre
 * d'erreur et la notification qui le liront arrivent à l'étape 52, avec leurs textes.
 *
 * **En mémoire seulement.** Un armement échoué est retenté à chaque démarrage du processus et à chaque
 * déverrouillage (`PlanChangeReason.PROCESS_START`, `USER_UNLOCKED`) : chaque processus neuf republie
 * donc l'état avant qu'un écran ne le lise, et un état persisté pourrait survivre à la cause corrigée.
 */
class PlanArmingStatus {
    private val mutableState = MutableStateFlow<PlanArmingState>(PlanArmingState.Ok)

    val state: StateFlow<PlanArmingState> = mutableState.asStateFlow()

    fun reportOk() {
        mutableState.value = PlanArmingState.Ok
    }

    fun reportFailed(
        failure: ActivationFailure,
        atEpochMillis: Long,
    ) {
        mutableState.value = PlanArmingState.Failed(failure, atEpochMillis)
    }
}
