package com.niumi.system.blocking

import com.niumi.database.blocking.BlockedPackagesState

/**
 * Prévenu quand la projection de blocage passe d'inactive à active pour une session
 * (SPEC_ANDROID §12.4, « application déjà ouverte à l'instant de début » ; Lot 6).
 *
 * L'algorithme de §12.2 ne décide qu'à chaque changement de fenêtre : une application bloquée déjà au
 * premier plan à 22:30 ne produirait aucun événement et resterait ouverte jusqu'au prochain
 * changement. Ce signal permet au service de rejouer sa décision sur le dernier package qu'il a vu.
 *
 * Appelé **une seule fois** par transition, et jamais sur une projection restée active ou devenue
 * illisible : chaque appel peut déclencher un `GLOBAL_ACTION_HOME`, que l'anti-rebond de §12.2 borne
 * mais qu'il ne faut pas provoquer sans raison.
 */
fun interface BlockingActivationListener {
    fun onBlockingActivated(state: BlockedPackagesState.Active)
}

/**
 * Prévenu quand la projection cesse de bloquer quoi que ce soit (étape 39 de la refonte) : l'écran 7
 * (SPEC_ANDROID §12.2) reste affiché jusqu'à « Fermer », et doit donc partir de lui-même quand le
 * scan a libéré la session. « Plus rien de bloqué » couvre `Inactive` comme une libération dont
 * `REMOVE_BLOCKING` a réussi (`Releasing` sans paquet effectif), qui précède l'état final.
 *
 * Appelé **une seule fois** par transition, et jamais sur une persistance illisible, qui ne lève pas
 * le blocage (§13).
 */
fun interface BlockingDeactivationListener {
    fun onBlockingDeactivated()
}
