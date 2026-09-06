package com.niumi.feature.session.blocking

import com.niumi.system.common.OperationResult

/**
 * Overlay explicatif affiché lorsqu'une application bloquée passe au premier plan
 * (SPEC_ANDROID §12.2). [isShowing] permet aux tests instrumentés d'observer l'état réel de la
 * fenêtre ajoutée par [WindowManagerBlockOverlayController], faute d'introspection visuelle
 * disponible dans la stack de test (pas d'UiAutomator déclaré).
 *
 * [show] renvoie un [OperationResult] comme tous les adaptateurs système du dépôt : une
 * exception qui traverserait `onAccessibilityEvent` ferait planter l'application **et**
 * conduirait Android à désactiver le service — donc à supprimer le blocage entier. L'overlay
 * est une aide à la compréhension ; son échec ne doit jamais emporter la fonction de blocage.
 */
interface BlockOverlayController {
    val isShowing: Boolean

    fun show(displayName: String): OperationResult

    fun hide()
}
