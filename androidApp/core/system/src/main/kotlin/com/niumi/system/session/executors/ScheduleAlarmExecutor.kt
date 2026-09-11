package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.common.OperationResult
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/** `SCHEDULE_ALARM`, requis pour `ACTIVATION_SUCCEEDED` (SPEC_CORE_KMP §6, §9.1). */
class ScheduleAlarmExecutor(
    private val alarmScheduler: AlarmScheduler,
    private val technicalEventLog: TechnicalEventLog,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome {
        val result =
            alarmScheduler.schedule(
                snapshot.sessionId,
                snapshot.revision,
                snapshot.wakeSchedule.triggerAtEpochMillis,
            )
        if (result is OperationResult.Success) {
            technicalEventLog.log(TechnicalEventType.ALARM_SCHEDULED, snapshot.sessionId)
        }
        return ExecutionOutcome(result)
    }
}
