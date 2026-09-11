package com.niumi.feature.setup.readiness

import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome

/**
 * Les deux contrôles de §13 qui ne décrivent pas l'état de l'appareil mais une étape du parcours
 * de préparation. `AndroidDeviceReadinessChecker.journeyChecks()` les traite déjà à part depuis
 * l'étape 12a : ils sont convertis vers les champs dédiés d'`ActivationPolicyInputDto`, pas vers
 * sa liste `checks`.
 */
private val JOURNEY_CHECK_IDS =
    setOf(ReadinessCheckId.PAIRED_BOX, ReadinessCheckId.APP_SELECTION)

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
    /**
     * Les deux étapes de parcours de §13 restent accessibles une fois satisfaites : ce ne sont pas
     * des remédiations mais des choix de l'utilisateur, que §11.1 et §12.1 l'autorisent à refaire
     * tant qu'aucune session n'est en cours. Sans cela leur bouton disparaîtrait avec leur échec,
     * et les écrans 3 et 4 deviendraient inatteignables — défaut mesuré sur appareil à l'étape 13.
     *
     * Les contrôles de blocage, eux, n'ont rien à rouvrir une fois satisfaits : un volume d'alarme
     * correct ne se « remodifie » pas depuis le diagnostic.
     */
    val isRevisitable: Boolean
        get() = outcome == ReadinessOutcome.PASSED && id in JOURNEY_CHECK_IDS

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
