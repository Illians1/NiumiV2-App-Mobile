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
 * Alarme de début du blocage différé (SPEC_ANDROID §9.1 seconde dérogation, §12.4 ; Lot 6).
 *
 * `setExactAndAllowWhileIdle` plutôt que `setAlarmClock`, pour la même raison que le watchdog de
 * §10.2 : un début de blocage à 22:30 n'est pas une alarme de l'utilisateur au sens du réglage
 * « prochaine alarme », et l'y afficher ferait croire que Niumi sonnera à cette heure. La mesure du
 * 2026-09-15 qui a écarté le quota Doze pour le watchdog vaut ici — `USE_EXACT_ALARM` affranchit
 * l'alarme des politiques `device_idle` et `app_standby` — et reste à confirmer sur appareil pour
 * cette alarme-ci (§20, Lot 6).
 *
 * Calqué sur [AndroidRingingWatchdog], à ceci près que l'instant est **contractuel** et non calculé :
 * `startsAtEpochMillis` est immuable après l'activation (SPEC_CORE_KMP §8.3), et la reprogrammation
 * reprend toujours le même (§9.3).
 */
class AndroidBlockingStartScheduler(
    private val context: Context,
    private val resolver: NiumiComponentResolver,
    private val pendingIntentFactory: AndroidPendingIntentFactory,
) : BlockingStartScheduler {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(
        sessionId: String,
        revision: Long,
        startsAtEpochMillis: Long,
    ): OperationResult =
        try {
            val pendingIntent =
                pendingIntentFactory.create(BlockingStartPendingIntentSpecs.blockingStart(sessionId, revision))
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                startsAtEpochMillis,
                pendingIntent,
            )
            OperationResult.Success
        } catch (error: SecurityException) {
            OperationResult.Failure("ANDROID_EXACT_ALARM_DENIED", error)
        }

    // Même patron qu'`AndroidRingingWatchdog.disarm` et `AndroidAlarmScheduler.cancel` :
    // `FLAG_NO_CREATE` pour ne créer aucun `PendingIntent` juste pour le retrouver, puis les deux
    // annulations (`AlarmManager` et le `PendingIntent` lui-même), faute de quoi un futur
    // `getBroadcast` avec les mêmes extras en récupérerait les résidus.
    override fun cancel(sessionId: String): OperationResult {
        val existing = existingPendingIntent(sessionId) ?: return OperationResult.AlreadySatisfied
        alarmManager.cancel(existing)
        existing.cancel()
        return OperationResult.Success
    }

    override fun isScheduled(sessionId: String): Boolean = existingPendingIntent(sessionId) != null

    /**
     * La révision ne participe pas au code de requête (elle n'est qu'un extra) : `revision = 0L`
     * suffit donc à retrouver le `PendingIntent` posé par [schedule], quelle qu'ait été la sienne.
     * Même convention qu'`AndroidAlarmScheduler.existingAlarmPendingIntent`.
     */
    private fun existingPendingIntent(sessionId: String): PendingIntent? {
        val spec = BlockingStartPendingIntentSpecs.blockingStart(sessionId, revision = 0L)
        val intent = Intent().apply { component = resolver.componentName(NiumiComponent.BLOCKING_START_RECEIVER) }
        return PendingIntent.getBroadcast(
            context,
            spec.requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
    }
}
