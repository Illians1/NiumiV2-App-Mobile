package com.niumi.system.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Déclenche la réconciliation au démarrage du processus (SPEC_ANDROID §9.2, dernier alinéa :
 * « Au démarrage du processus, `SessionReconciler` traite tout état `PREPARING` resté
 * incomplet » ; §13.1, qui liste `PROCESS_START` parmi les déclencheurs de la surveillance).
 *
 * Vit au niveau de l'`Application` et non d'une activité : le processus peut être réveillé sans
 * interface, par le receveur d'alarme notamment, et la reprise d'une transaction interrompue ne
 * doit pas attendre qu'un écran s'ouvre. C'est aussi ce qui republie le snapshot d'une session
 * existante après un redémarrage — sans quoi l'accueil affiche « Aucune session » alors que
 * l'alarme est programmée (défaut mesuré sur appareil à l'étape 14).
 */
class SessionStartupReconciler(
    private val coordinator: SessionCoordinator,
    dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    fun reconcileAsync() {
        scope.launch { coordinator.reconcile(ReconcileReason.PROCESS_START) }
    }
}
