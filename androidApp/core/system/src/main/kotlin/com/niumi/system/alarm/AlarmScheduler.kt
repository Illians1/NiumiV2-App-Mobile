package com.niumi.system.alarm

import com.niumi.system.common.OperationResult

/** Contrat de programmation de l'alarme exacte (« Interfaces transverses » du plan MVP). */
interface AlarmScheduler {
    fun schedule(
        sessionId: String,
        revision: Long,
        triggerAtEpochMillis: Long,
    ): OperationResult

    fun cancel(sessionId: String): OperationResult

    // PendingIntent.getBroadcast(..., FLAG_NO_CREATE) != null
    fun isScheduled(sessionId: String): Boolean
}
