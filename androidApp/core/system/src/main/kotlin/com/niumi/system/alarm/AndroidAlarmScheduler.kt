package com.niumi.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.niumi.system.common.OperationResult
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.NiumiComponentResolver

/**
 * Unique API de réveil autorisée (SPEC_ANDROID §9.1) : `AlarmManager.setAlarmClock()` avec des
 * `PendingIntent` explicites et `FLAG_IMMUTABLE`. Aucun `WorkManager`, `Handler` ni
 * `setInexactRepeating()`.
 */
class AndroidAlarmScheduler(
    private val context: Context,
    private val resolver: NiumiComponentResolver,
    private val pendingIntentFactory: AndroidPendingIntentFactory,
) : AlarmScheduler {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(
        sessionId: String,
        revision: Long,
        triggerAtEpochMillis: Long,
    ): OperationResult =
        try {
            val alarmPendingIntent =
                pendingIntentFactory.create(AlarmPendingIntentSpecs.alarm(sessionId, revision))
            val showPendingIntent =
                pendingIntentFactory.create(AlarmPendingIntentSpecs.show(sessionId))
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtEpochMillis, showPendingIntent),
                alarmPendingIntent,
            )
            OperationResult.Success
        } catch (error: SecurityException) {
            OperationResult.Failure("ANDROID_EXACT_ALARM_DENIED", error)
        }

    override fun cancel(sessionId: String): OperationResult {
        val existing = existingAlarmPendingIntent(sessionId) ?: return OperationResult.AlreadySatisfied
        alarmManager.cancel(existing)
        existing.cancel()
        return OperationResult.Success
    }

    override fun isScheduled(sessionId: String): Boolean = existingAlarmPendingIntent(sessionId) != null

    private fun existingAlarmPendingIntent(sessionId: String): PendingIntent? {
        val spec = AlarmPendingIntentSpecs.alarm(sessionId, revision = 0L)
        val intent = Intent().apply { component = resolver.componentName(NiumiComponent.ALARM_RECEIVER) }
        return PendingIntent.getBroadcast(
            context,
            spec.requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
    }
}
