package com.niumi.system.alarm

import android.app.PendingIntent
import com.niumi.system.intent.IntentExtraValue
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.PendingIntentSpec

/**
 * Construit les trois `PendingIntent` d'une alarme (SPEC_ANDROID §9.1) : le déclencheur qui
 * cible `AlarmReceiver`, celui qui ouvre l'accueil (`showPendingIntent`) et celui du
 * full-screen intent qui ouvre `AlarmActivity`. Le code de requête est `sessionId.hashCode()`
 * pour les trois : stable pour une session donnée, ce qui garantit que `cancel()` retrouve et
 * annule exactement le `PendingIntent` programmé par `schedule()`.
 */
object AlarmPendingIntentSpecs {
    private const val IMMUTABLE_UPDATE_CURRENT =
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun alarm(
        sessionId: String,
        revision: Long,
    ): PendingIntentSpec =
        PendingIntentSpec(
            kind = PendingIntentSpec.Kind.BROADCAST,
            target = NiumiComponent.ALARM_RECEIVER,
            requestCode = requestCodeFor(sessionId),
            flags = IMMUTABLE_UPDATE_CURRENT,
            // `revision` est un nombre, pas une chaîne : `AlarmReceiver` la relit avec
            // `getLongExtra`, qui renverrait silencieusement sa valeur par défaut si l'extra
            // était écrit comme texte — la commande serait alors rejetée et l'alarme resterait
            // muette (constaté sur appareil, voir `ETAPE-03.md`).
            extras =
                mapOf(
                    "sessionId" to IntentExtraValue.Text(sessionId),
                    "revision" to IntentExtraValue.Number(revision),
                ),
        )

    fun show(sessionId: String): PendingIntentSpec =
        PendingIntentSpec(
            kind = PendingIntentSpec.Kind.ACTIVITY,
            target = NiumiComponent.MAIN_ACTIVITY,
            requestCode = requestCodeFor(sessionId),
            flags = IMMUTABLE_UPDATE_CURRENT,
            extras = mapOf("sessionId" to IntentExtraValue.Text(sessionId)),
        )

    fun fullScreen(sessionId: String): PendingIntentSpec =
        PendingIntentSpec(
            kind = PendingIntentSpec.Kind.ACTIVITY,
            target = NiumiComponent.ALARM_ACTIVITY,
            requestCode = requestCodeFor(sessionId),
            flags = IMMUTABLE_UPDATE_CURRENT,
            extras = mapOf("sessionId" to IntentExtraValue.Text(sessionId)),
        )

    private fun requestCodeFor(sessionId: String): Int = sessionId.hashCode()
}
