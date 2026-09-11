package com.niumi.system.session.fakes

import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType

class FakeTechnicalEventLog : TechnicalEventLog {
    val logged = mutableListOf<TechnicalEventType>()

    override fun log(
        type: TechnicalEventType,
        sessionId: String?,
        detailsJson: String?,
    ) {
        logged += type
    }

    override suspend fun recent(): List<TechnicalEventEntry> = emptyList()
}
