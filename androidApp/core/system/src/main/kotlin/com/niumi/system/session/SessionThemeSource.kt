package com.niumi.system.session

import com.niumi.core.interop.SessionSnapshotDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Thème clair ou sombre de l'app, d'après la session persistée (étape 33, [isSessionDark]).
 *
 * Dans un processus neuf, [SessionSnapshotPublisher.snapshot] vaut `null` tant que personne n'a
 * lu la persistance : un `null` en première valeur ne prouve pas l'absence de session. Il est
 * remplacé par une lecture de [SessionPersistenceGateway.load] (même règle qu'`AlarmViewModel`) ;
 * ensuite, le publisher fait foi. Une persistance illisible se lit clair : aucun état sombre n'est
 * prouvé. Une exception que la passerelle ne convertit pas se lit de même : le thème ne doit pas
 * faire tomber l'app avant son premier écran.
 *
 * `MainActivity` garde l'écran de démarrage tant que ce thème est inconnu (décision de Mehdi du
 * 2026-10-19). La lecture est donc bornée à [LAUNCH_READ_TIMEOUT_MILLIS] : au-delà, l'app s'ouvre
 * en clair plutôt que de rester sur l'écran de démarrage. Garde-fou jamais atteint en usage
 * normal (lecture en quelques millisecondes).
 */
class SessionThemeSource
    @Inject
    constructor(
        gateway: SessionPersistenceGateway,
        publisher: SessionSnapshotPublisher,
    ) {
        val dark: Flow<Boolean> =
            publisher.snapshot
                .withIndex()
                .map { (index, snapshot) ->
                    if (index == 0 && snapshot == null) {
                        persistedSnapshot(gateway)
                    } else {
                        snapshot
                    }
                }.map { it.isSessionDark() }
                .distinctUntilChanged()
    }

@Suppress("TooGenericExceptionCaught") // Toute exception non prévue se lit « aucun état sombre prouvé ».
private suspend fun persistedSnapshot(gateway: SessionPersistenceGateway): SessionSnapshotDto? =
    try {
        withTimeoutOrNull(LAUNCH_READ_TIMEOUT_MILLIS) { gateway.load() as? LoadResult.Present }?.snapshot
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

/** Attente maximale de l'écran de démarrage (décision de Mehdi du 2026-10-19 : 3 secondes). */
const val LAUNCH_READ_TIMEOUT_MILLIS = 3_000L
