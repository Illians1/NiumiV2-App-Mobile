package com.niumi.feature.session.diagnostics

import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType

/**
 * Double de [TechnicalEventLog] qui **rend** ce qu'on lui donne. `RecordingTechnicalEventLog`
 * (écran 7) ne retient que les types écrits et renvoie une liste vide : l'écran 12 est le premier
 * consommateur de `recent()`, il lui faut un double capable de relire.
 */
class ReplayingTechnicalEventLog(
    var entries: List<TechnicalEventEntry> = emptyList(),
) : TechnicalEventLog {
    val logged = mutableListOf<TechnicalEventType>()

    override fun log(
        type: TechnicalEventType,
        sessionId: String?,
        detailsJson: String?,
    ) {
        logged += type
    }

    override suspend fun recent(): List<TechnicalEventEntry> = entries
}
