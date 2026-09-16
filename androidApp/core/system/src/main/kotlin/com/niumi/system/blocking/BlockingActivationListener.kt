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
