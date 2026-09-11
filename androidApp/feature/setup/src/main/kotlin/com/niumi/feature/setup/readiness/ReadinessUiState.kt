package com.niumi.feature.setup.readiness

import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome

/**
 * Un contrôle affichable. [isActionAvailable] vaut `false` quand le recours n'existe pas encore
 * dans l'application : le blocage est alors énoncé sans proposer un bouton qui ne mènerait nulle
 * part, ce qui serait un faux état de fiabilité (SPEC_ANDROID §15).
 */
data class ReadinessItem(
    val id: ReadinessCheckId,
    val message: String,
    val label: String,
    val severity: ReadinessSeverityDto,
    val outcome: ReadinessOutcome,
    val action: ReadinessAction,
    val actionLabel: String,
    val isActionAvailable: Boolean,
) {
    val isBlocking: Boolean
        get() = outcome == ReadinessOutcome.FAILED && severity != ReadinessSeverityDto.WARNING

    /**
     * Ce que l'écran affiche. Un contrôle satisfait est **nommé** ; seul un contrôle en échec
     * porte son message de remédiation. Afficher « ✓ Le volume des alarmes est à zéro » pour un
     * volume correct énoncerait l'inverse de la vérité (§15).
     */
    val summary: String
        get() = if (outcome == ReadinessOutcome.PASSED) label else message
}

/**
 * État d'affichage du diagnostic (SPEC_ANDROID §13). [primary] est le premier contrôle en échec
 * dans l'ordre du tableau de §13 : l'écran n'affiche qu'une action principale à la fois.
 *
 * [isAllowed] vient exclusivement de `NiumiCoreFacade.evaluateActivation` : c'est le verdict
 * commun, pas une règle Android. Il peut être `false` alors qu'aucun contrôle affiché n'échoue,
 * lorsque l'heure de réveil n'a pas encore été choisie — [isDeviceReady] distingue alors « cet
 * appareil est prêt » de « cette session peut être armée ».
 */
data class ReadinessUiState(
    val items: List<ReadinessItem> = emptyList(),
    val primary: ReadinessItem? = null,
    val isAllowed: Boolean = false,
    val isLoading: Boolean = true,
    val nowEpochMillis: Long = 0L,
) {
    val isDeviceReady: Boolean get() = items.none { it.isBlocking }
}
