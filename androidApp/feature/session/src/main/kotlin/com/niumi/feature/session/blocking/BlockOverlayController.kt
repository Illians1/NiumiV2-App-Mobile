package com.niumi.feature.session.blocking

import com.niumi.system.common.OperationResult

/**
 * Écran 7 · Écran de blocage (SPEC_ANDROID §12.2, étape 39 de la refonte), affiché par-dessus
 * l'accueil du téléphone quand une application bloquée passe au premier plan. Il reste affiché
 * jusqu'à « Fermer » ou « Ouvrir Niumi » (Q6, décision de Mehdi du 2026-10-22), et le service le
 * retire aussi à la fin du blocage, à l'extinction de l'écran, à l'interruption et à la déliaison.
 * [isShowing] permet aux tests instrumentés d'observer l'état réel de la fenêtre.
 *
 * [show] renvoie un [OperationResult] comme tous les adaptateurs système du dépôt : une
 * exception qui traverserait `onAccessibilityEvent` ferait planter l'application **et**
 * conduirait Android à désactiver le service — donc à supprimer le blocage entier. L'écran 7
 * est une explication ; son échec ne doit jamais emporter la fonction de blocage.
 */
interface BlockOverlayController {
    val isShowing: Boolean

    /**
     * [appName] : libellé de l'application bloquée, `null` s'il manque. Déjà affiché, l'écran prend
     * le nouveau nom sans seconde fenêtre (`AlreadySatisfied`).
     */
    fun show(appName: String?): OperationResult

    /** Idempotent. */
    fun hide()
}
