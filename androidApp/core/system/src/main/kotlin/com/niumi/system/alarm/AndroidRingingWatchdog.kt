package com.niumi.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.niumi.system.common.Clock
import com.niumi.system.common.OperationResult
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.NiumiComponentResolver

/**
 * Alarme de secours pendant `RINGING` (SPEC_ANDROID §9.1, §10.2 ; étape 20).
 * `setExactAndAllowWhileIdle` plutôt que `setAlarmClock` : un tic invisible toutes les 60 s ne doit
 * pas s'afficher au réglage « prochaine alarme » du système, contrairement au réveil lui-même —
 * §10.5 exige déjà la même discrétion pour la notification d'attente de scan.
 *
 * **Le quota Doze annoncé par la documentation — une livraison par application toutes les neuf
 * minutes — a été mesuré puis écarté** (2026-09-15, Xiaomi 25080RABDG / Android 16, Doze profond
 * forcé, processus tué pendant `RINGING`) : cinq tics consécutifs délivrés à l'heure, 58 à 62 s
 * d'intervalle, appareil en `IDLE`. `USE_EXACT_ALARM` en est la raison
 * (`exactAllowReason=policy_permission`), et `dumpsys alarm` montre les politiques `device_idle` et
 * `app_standby` sans contrainte sur cette alarme. [AlarmManager.setAlarmClock] n'est donc pas
 * nécessaire, et sa visibilité au réglage « prochaine alarme » évitée. Réserve résiduelle en §4.2 :
 * le seau d'App Standby n'a jamais pu être rétrogradé sous `EXEMPTED` pendant l'essai.
 */
class AndroidRingingWatchdog(
    private val context: Context,
    private val resolver: NiumiComponentResolver,
    private val pendingIntentFactory: AndroidPendingIntentFactory,
    private val clock: Clock,
) : RingingWatchdog {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun arm(sessionId: String): OperationResult =
        try {
            val pendingIntent = pendingIntentFactory.create(RingingWatchdogSpecs.pendingIntent(sessionId))
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                RingingWatchdogSpecs.nextTriggerAt(clock.nowEpochMillis()),
                pendingIntent,
            )
            OperationResult.Success
        } catch (error: SecurityException) {
            OperationResult.Failure("ANDROID_EXACT_ALARM_DENIED", error)
        }

    // Même patron qu'`AndroidAlarmScheduler.cancel` : `FLAG_NO_CREATE` pour ne créer aucun
    // `PendingIntent` juste pour le retrouver, puis les deux annulations (`AlarmManager` et le
    // `PendingIntent` lui-même) sont nécessaires pour qu'un futur `getBroadcast` avec les mêmes
    // extras n'en récupère pas les résidus.
    override fun disarm(sessionId: String): OperationResult {
        val existing = existingPendingIntent(sessionId) ?: return OperationResult.AlreadySatisfied
        alarmManager.cancel(existing)
        existing.cancel()
        return OperationResult.Success
    }

    private fun existingPendingIntent(sessionId: String): PendingIntent? {
        val spec = RingingWatchdogSpecs.pendingIntent(sessionId)
        val intent = Intent().apply { component = resolver.componentName(NiumiComponent.RINGING_WATCHDOG_RECEIVER) }
        return PendingIntent.getBroadcast(
            context,
            spec.requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
    }
}
