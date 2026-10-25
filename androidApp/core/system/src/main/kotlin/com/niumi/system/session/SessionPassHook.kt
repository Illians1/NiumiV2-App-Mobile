package com.niumi.system.session

/**
 * Ce qui suit une passe du coordinateur, **sous le même verrou** (étape 44) : le `WakePlanScheduler`
 * y tire les conséquences d'une session finie (`COMPLETED` → occurrence suivante, `CANCELLED` →
 * interrupteur éteint), ré-arme au démarrage ou au déverrouillage, et recalcule après un déplacement
 * de l'horloge ou du fuseau.
 *
 * Appelé par [HookedSessionCoordinator] après [SessionCoordinator.dispatch] et
 * [SessionCoordinator.reconcile] seulement, jamais depuis [SessionCoordinator.withLock] : ce que le
 * scheduler fait lui-même sous le verrou (désarmer puis armer) ne repasse donc pas par ici.
 */
interface SessionPassHook {
    suspend fun afterDispatch(
        result: DispatchResult,
        session: LockedSession,
    )

    /** Les actions renvoyées s'ajoutent à celles de la passe, dans [ReconcileResult.actions]. */
    suspend fun afterReconcile(
        reason: ReconcileReason,
        result: ReconcileResult,
        session: LockedSession,
    ): List<ReconcileAction>
}
