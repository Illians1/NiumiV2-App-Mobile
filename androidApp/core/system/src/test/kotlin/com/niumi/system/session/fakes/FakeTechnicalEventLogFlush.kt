package com.niumi.system.session.fakes

import com.niumi.database.logging.TechnicalEventLogFlush

class FakeTechnicalEventLogFlush(
    private val journal: CallJournal? = null,
) : TechnicalEventLogFlush {
    var callCount: Int = 0
        private set

    override suspend fun flush() {
        journal?.record("TechnicalEventLogFlush.flush")
        callCount++
    }
}
