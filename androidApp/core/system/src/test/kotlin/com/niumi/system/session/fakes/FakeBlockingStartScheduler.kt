package com.niumi.system.session.fakes

import com.niumi.system.alarm.BlockingStartScheduler
import com.niumi.system.common.OperationResult

/**
 * Faux programmateur de l'alarme de début (Lot 6), calqué sur [FakeAlarmScheduler] : il retient les
 * sessions programmées pour que `isScheduled` dise la vérité, et l'instant de la dernière
 * programmation, que la réconciliation doit reprendre **à l'identique** (SPEC_ANDROID §9.3).
 */
class FakeBlockingStartScheduler(
    private val journal: CallJournal? = null,
) : BlockingStartScheduler {
    var scheduleResult: OperationResult = OperationResult.Success
    var cancelResult: OperationResult = OperationResult.Success
    var lastScheduledAtEpochMillis: Long? = null
    var scheduleCount: Int = 0
        private set
    private val scheduled = mutableSetOf<String>()

    override fun schedule(
        sessionId: String,
        revision: Long,
        startsAtEpochMillis: Long,
    ): OperationResult {
        journal?.record("BlockingStartScheduler.schedule")
        scheduleCount++
        if (scheduleResult is OperationResult.Success) {
            scheduled += sessionId
            lastScheduledAtEpochMillis = startsAtEpochMillis
        }
        return scheduleResult
    }

    override fun cancel(sessionId: String): OperationResult {
        journal?.record("BlockingStartScheduler.cancel")
        if (cancelResult !is OperationResult.Failure) scheduled -= sessionId
        return cancelResult
    }

    override fun isScheduled(sessionId: String): Boolean = sessionId in scheduled

    /** Simule une alarme effacée par la surcouche OEM, sans passer par `cancel` (§18). */
    fun forget(sessionId: String) {
        scheduled -= sessionId
    }
}
