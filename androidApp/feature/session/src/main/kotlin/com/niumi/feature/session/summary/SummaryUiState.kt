package com.niumi.feature.session.summary

import com.niumi.database.BlockedPackage
import com.niumi.feature.session.ui.AlarmSoundSummary
import com.niumi.feature.session.ui.AlarmSoundTexts
import com.niumi.feature.session.ui.WakeScheduleDisplay
import com.niumi.system.audio.AlarmSoundSettings

/**
 * État de l'écran 6 (SPEC_ANDROID §15). [canActivate] vient exclusivement du dernier verdict de
 * `NiumiCoreFacade.evaluateActivation` relayé par `ArmSessionUseCase.preview` : l'écran ne
 * réimplémente aucune règle d'activation (§19.1, « impossibilité de confirmer si un contrôle
 * bloquant échoue »).
 */
data class SummaryUiState(
    val display: WakeScheduleDisplay? = null,
    val blockingDisplay: WakeScheduleDisplay? = null,
    val blockedPackages: List<BlockedPackage> = emptyList(),
    /** Lot 8 : remplace `boxId`, les noms de la liste figée, dans l'ordre de `PairedBoxStore.all()`. */
    val boxNames: List<String> = emptyList(),
    val isAllowed: Boolean = false,
    val isActivating: Boolean = false,
    val isSessionInProgress: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null,
    /** Ligne « Sonnerie » (§9) : résumé de la préférence, pas de la session future. */
    val alarmSoundSummary: AlarmSoundSummary = AlarmSoundTexts.summary(AlarmSoundSettings()),
) {
    /**
     * [isActivating] désactive le bouton pendant l'appel : `dispatch` et `reconcile` partagent un
     * mutex non réentrant, l'activation peut durer plusieurs centaines de millisecondes et un
     * double appui créerait deux sessions avec deux `sessionId` distincts.
     */
    val canActivate: Boolean
        get() = isAllowed && !isActivating && !isSessionInProgress && display != null

    /**
     * Lot 6 : aucun instant de début à afficher signifie « dès l'activation » (SPEC_ANDROID §15,
     * écran 6). Dérivé de [blockingDisplay] plutôt que porté en propre, pour qu'un affichage et son
     * libellé ne puissent jamais se contredire.
     */
    val isBlockingImmediate: Boolean
        get() = blockingDisplay == null

    /** Noms joints pour l'affichage (« Salle de bain, Cuisine »), `null` sans boîtier (Lot 8). */
    val boxNamesDisplay: String?
        get() = boxNames.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
