package com.niumi.system.session.fakes

import com.niumi.core.interop.SessionDecisionDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.system.session.SessionReducer

/** Compte les appels à [reduce] : preuve qu'un doublon strict n'appelle jamais le moteur. */
class RecordingSessionReducer(
    private val delegate: SessionReducer,
) : SessionReducer {
    var callCount: Int = 0
        private set

    override fun reduce(
        snapshot: SessionSnapshotDto?,
        event: SessionEventDto,
    ): SessionDecisionDto {
        callCount++
        return delegate.reduce(snapshot, event)
    }
}
