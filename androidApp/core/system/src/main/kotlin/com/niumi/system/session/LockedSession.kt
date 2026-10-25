package com.niumi.system.session

import com.niumi.core.interop.SessionEventDto
import com.niumi.database.AndroidSessionExtras

/**
 * Accès à la session **sous le verrou déjà pris** par [SessionCoordinator.withLock] (étape 44). Le
 * mutex du coordinateur n'est pas réentrant : appeler [SessionCoordinator.dispatch] depuis un bloc
 * verrouillé bloquerait à jamais. Ces fonctions sont les versions qui ne le reprennent pas, pour
 * enchaîner plusieurs décisions sans qu'aucun autre événement ne s'intercale — désarmer puis armer la
 * suivante, effacer le pointeur d'une session finie puis activer (point de vigilance 8 du plan de
 * refonte).
 *
 * Ne vit que le temps du bloc : la conserver au-delà reviendrait à agir sans verrou.
 */
interface LockedSession {
    suspend fun load(): LoadResult

    suspend fun clearActive(sessionId: String)

    suspend fun dispatch(
        event: SessionEventDto,
        extras: AndroidSessionExtras? = null,
    ): DispatchResult

    suspend fun reconcile(reason: ReconcileReason): ReconcileResult
}
