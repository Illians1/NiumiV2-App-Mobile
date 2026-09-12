package com.niumi.database.incident

import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.NiumiDatabase
import com.niumi.database.directboot.UnlockState
import com.niumi.database.mapping.toDomain
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Lecture des incidents d'une session. Premier consommateur d'`IncidentDao`, qui n'avait qu'un
 * écrivain (`RecordIncidentExecutor`, étape 11) : l'écran de session active doit présenter les
 * incidents `CRITICAL` explicitement (SPEC_CORE_KMP §7.3).
 *
 * Interface dédiée plutôt qu'une méthode de plus sur `SessionStore` : cette classe est au plafond
 * `TooManyFunctions` de detekt (11) depuis l'étape 11, et la lecture d'incidents n'entre dans
 * aucune transaction de décision.
 */
interface SessionIncidentsReader {
    suspend fun incidents(sessionId: String): List<SessionIncidentDto>
}

/**
 * Avant déverrouillage, la liste est vide : le snapshot Direct Boot ne porte pas de table
 * d'incidents (SPEC_ANDROID §7.3), et `SessionPersistenceGateway.recordIncident` diffère déjà
 * leur écriture jusqu'à `USER_UNLOCKED`. Vide et non erreur : l'écran qui les affiche n'est de
 * toute façon atteignable qu'appareil déverrouillé, et un incident manquant ne doit jamais
 * empêcher d'afficher le reste de la session.
 */
@Singleton
class RoomSessionIncidentsReader
    @Inject
    constructor(
        private val unlockState: UnlockState,
        private val databaseProvider: Provider<NiumiDatabase>,
    ) : SessionIncidentsReader {
        override suspend fun incidents(sessionId: String): List<SessionIncidentDto> {
            if (!unlockState.isUserUnlocked) return emptyList()
            return databaseProvider
                .get()
                .incidentDao()
                .forSession(sessionId)
                .map { it.toDomain() }
        }
    }
