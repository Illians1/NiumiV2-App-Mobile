package com.niumi.system.session.executors

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.ExecutionOutcome

/** `CANCEL_ALARM`, requis pour `RELEASE_SUCCEEDED` (SPEC_CORE_KMP §6, §12). */
class CancelAlarmExecutor(
    private val alarmScheduler: AlarmScheduler,
) : EffectExecutor {
    override suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome = ExecutionOutcome(alarmScheduler.cancel(snapshot.sessionId))
}
