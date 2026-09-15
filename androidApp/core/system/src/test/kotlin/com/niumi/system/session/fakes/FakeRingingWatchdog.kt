package com.niumi.system.session.fakes

import com.niumi.system.alarm.RingingWatchdog
import com.niumi.system.common.OperationResult

class FakeRingingWatchdog(
    private val journal: CallJournal? = null,
) : RingingWatchdog {
    private val armedSessions = mutableSetOf<String>()
    val armed: Set<String> get() = armedSessions

    override fun arm(sessionId: String): OperationResult {
        journal?.record("RingingWatchdog.arm")
        armedSessions += sessionId
        return OperationResult.Success
    }

    override fun disarm(sessionId: String): OperationResult {
        journal?.record("RingingWatchdog.disarm")
        armedSessions -= sessionId
        return OperationResult.Success
    }
}
