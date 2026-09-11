package com.niumi.system.session

import com.niumi.core.interop.SessionEventDto
import com.niumi.database.AndroidSessionExtras

/**
 * Autorité unique des transitions natives (« Interfaces transverses » du plan MVP, étendue à
 * l'étape 11) : convertit un fait système en événement KMP, persiste atomiquement snapshot, reçu
 * et effets avant d'exécuter le moindre effet, puis referme la phase par
 * `ACTIVATION_SUCCEEDED`/`FAILED` ou `RELEASE_SUCCEEDED`/`FAILED` selon SPEC_CORE_KMP §6, §10, §12.
 *
 * [extras] porte les données Android absentes de [SessionEventDto] (boîtier figé, sonnerie,
 * sélection d'applications) : requis uniquement pour `ACTIVATION_REQUESTED`, ignoré sinon — la
 * session déjà persistée fait foi (`RoomSessionStore.freezeFrom`).
 */
interface SessionCoordinator {
    suspend fun dispatch(
        event: SessionEventDto,
        extras: AndroidSessionExtras? = null,
    ): DispatchResult

    suspend fun reconcile(reason: ReconcileReason): ReconcileResult
}
