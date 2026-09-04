package com.niumi.system.ringing

import com.niumi.system.common.OperationResult

/** Démarre/arrête `AlarmRingingService` (« Interfaces transverses » du plan MVP). */
interface RingingController {
    fun startRinging(
        sessionId: String,
        revision: Long,
    ): OperationResult

    fun stopRinging(sessionId: String): OperationResult
}
