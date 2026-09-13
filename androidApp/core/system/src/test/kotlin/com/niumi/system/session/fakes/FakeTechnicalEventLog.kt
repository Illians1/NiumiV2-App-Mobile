package com.niumi.system.session.fakes

import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType

class FakeTechnicalEventLog : TechnicalEventLog {
    val logged = mutableListOf<TechnicalEventType>()

    /**
     * Les mêmes entrées avec leur `sessionId`. [logged] ne le portait pas, et l'étape 17 doit
     * prouver qu'un `ALARM_RECEIVED` issu d'extras invalides est journalisé **sans** identifiant
     * de session (SPEC_ANDROID §16, §17).
     */
    val entries = mutableListOf<Pair<TechnicalEventType, String?>>()

    override fun log(
        type: TechnicalEventType,
        sessionId: String?,
        detailsJson: String?,
    ) {
        logged += type
        entries += type to sessionId
    }

    override suspend fun recent(): List<TechnicalEventEntry> = emptyList()
}
