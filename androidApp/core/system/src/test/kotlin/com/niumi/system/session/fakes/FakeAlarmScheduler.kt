package com.niumi.system.session.fakes

import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.common.OperationResult

class FakeAlarmScheduler(
    private val journal: CallJournal? = null,
) : AlarmScheduler {
    var scheduleResult: OperationResult = OperationResult.Success
    var cancelResult: OperationResult = OperationResult.Success
    var canScheduleExactValue: Boolean = true
    private val scheduled = mutableSetOf<String>()

    override fun schedule(
        sessionId: String,
        revision: Long,
        triggerAtEpochMillis: Long,
    ): OperationResult {
        journal?.record("AlarmScheduler.schedule")
        if (scheduleResult is OperationResult.Success) scheduled += sessionId
        return scheduleResult
    }

    override fun cancel(sessionId: String): OperationResult {
        journal?.record("AlarmScheduler.cancel")
        if (cancelResult !is OperationResult.Failure) scheduled -= sessionId
        return cancelResult
    }

    override fun isScheduled(sessionId: String): Boolean = sessionId in scheduled

    override fun canScheduleExact(): Boolean = canScheduleExactValue
}
