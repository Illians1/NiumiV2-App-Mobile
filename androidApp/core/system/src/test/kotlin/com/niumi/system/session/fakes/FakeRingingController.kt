package com.niumi.system.session.fakes

import com.niumi.system.common.OperationResult
import com.niumi.system.ringing.RingingController

class FakeRingingController(
    private val journal: CallJournal? = null,
) : RingingController {
    var startResult: OperationResult = OperationResult.Success
    var stopResult: OperationResult = OperationResult.Success

    override fun startRinging(
        sessionId: String,
        revision: Long,
    ): OperationResult {
        journal?.record("RingingController.startRinging")
        return startResult
    }

    override fun stopRinging(sessionId: String): OperationResult {
        journal?.record("RingingController.stopRinging")
        return stopResult
    }
}
