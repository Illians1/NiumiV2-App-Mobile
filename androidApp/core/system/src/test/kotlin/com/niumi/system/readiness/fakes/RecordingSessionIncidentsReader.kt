package com.niumi.system.readiness.fakes

import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.incident.SessionIncidentsReader

/**
 * Double de [SessionIncidentsReader] alimenté par les incidents que le test a réellement
 * dispatchés : la déduplication du monitor se juge sur ce qui est **en base**, pas sur une liste
 * posée à la main.
 *
 * [unreadable] reproduit l'avant-déverrouillage, où le vrai lecteur renvoie une liste vide sans
 * pouvoir dire si des incidents existent (SPEC_ANDROID §7.3).
 */
class RecordingSessionIncidentsReader(
    var unreadable: Boolean = false,
) : SessionIncidentsReader {
    private val recorded = mutableMapOf<String, MutableList<SessionIncidentDto>>()

    fun record(
        sessionId: String,
        incident: SessionIncidentDto,
    ) {
        recorded.getOrPut(sessionId) { mutableListOf() } += incident
    }

    override suspend fun incidents(sessionId: String): List<SessionIncidentDto> =
        if (unreadable) emptyList() else recorded[sessionId].orEmpty().toList()
}
