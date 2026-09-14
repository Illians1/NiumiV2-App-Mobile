package com.niumi.system.session.fakes

import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.incident.SessionIncidentsReader

/**
 * Lecteur d'incidents branché sur ce que la passerelle a **réellement** enregistré. La
 * déduplication des incidents de changement d'heure (`SessionReconciler.reportClockChange`) se juge
 * ainsi de bout en bout : l'incident est dispatché, l'effet `RECORD_INCIDENT` l'écrit dans la
 * passerelle, et la passe suivante le retrouve ici. Un double alimenté à la main prouverait la
 * garde sans prouver que le chemin d'écriture y mène.
 *
 * [unreadable] reproduit l'avant-déverrouillage, où le vrai lecteur renvoie une liste vide sans
 * pouvoir dire si des incidents existent (SPEC_ANDROID §7.3).
 */
class GatewayIncidentsReader(
    private val gateway: InMemoryPersistenceGateway,
    var unreadable: Boolean = false,
) : SessionIncidentsReader {
    override suspend fun incidents(sessionId: String): List<SessionIncidentDto> =
        if (unreadable) {
            emptyList()
        } else {
            gateway.incidentsRecorded.filter { it.first == sessionId }.map { it.second }
        }
}
