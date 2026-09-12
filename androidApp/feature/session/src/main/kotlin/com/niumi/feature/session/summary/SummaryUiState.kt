package com.niumi.feature.session.summary

import com.niumi.database.BlockedPackage
import com.niumi.feature.session.ui.WakeScheduleDisplay

/** Nombre de caractères du `boxId` affichés : jamais l'identifiant complet, jamais le token (§16). */
private const val BOX_ID_PREFIX_LENGTH = 8

/**
 * État de l'écran 6 (SPEC_ANDROID §15). [canActivate] vient exclusivement du dernier verdict de
 * `NiumiCoreFacade.evaluateActivation` relayé par `ArmSessionUseCase.preview` : l'écran ne
 * réimplémente aucune règle d'activation (§19.1, « impossibilité de confirmer si un contrôle
 * bloquant échoue »).
 */
data class SummaryUiState(
    val display: WakeScheduleDisplay? = null,
    val blockedPackages: List<BlockedPackage> = emptyList(),
    val boxId: String? = null,
    val isAllowed: Boolean = false,
    val isActivating: Boolean = false,
    val isSessionInProgress: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null,
) {
    /**
     * [isActivating] désactive le bouton pendant l'appel : `dispatch` et `reconcile` partagent un
     * mutex non réentrant, l'activation peut durer plusieurs centaines de millisecondes et un
     * double appui créerait deux sessions avec deux `sessionId` distincts.
     */
    val canActivate: Boolean
        get() = isAllowed && !isActivating && !isSessionInProgress && display != null

    /** `boxId` tronqué (§16 : jamais l'identifiant complet à l'écran, jamais l'empreinte du token). */
    val truncatedBoxId: String?
        get() = boxId?.take(BOX_ID_PREFIX_LENGTH)
}
