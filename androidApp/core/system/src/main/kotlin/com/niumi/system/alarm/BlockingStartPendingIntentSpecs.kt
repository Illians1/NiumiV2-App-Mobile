package com.niumi.system.alarm

import android.app.PendingIntent
import com.niumi.system.intent.IntentExtraValue
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.PendingIntentSpec

/**
 * `PendingIntent` de l'alarme de début du blocage différé (SPEC_ANDROID §9.1, §12.4 ; Lot 6).
 *
 * Le code de requête est salé, donc **distinct** de celui du réveil ([AlarmPendingIntentSpecs], qui
 * utilise `sessionId.hashCode()` nu) et de celui du watchdog ([RingingWatchdogSpecs], salé par
 * `"WATC"`) pour une même session : les trois alarmes doivent apparaître séparément dans
 * `dumpsys alarm`, et `AlarmManager.cancel()` ne doit jamais pouvoir atteindre l'une en visant
 * l'autre — annuler le réveil en croyant annuler le début du blocage rendrait Niumi muet au matin.
 */
object BlockingStartPendingIntentSpecs {
    private const val IMMUTABLE_UPDATE_CURRENT =
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    // Octets ASCII de "BLKS", arbitraire mais stable, sur le modèle du sel "WATC" du watchdog.
    private const val REQUEST_CODE_SALT = 0x424C4B53

    /**
     * `revision` est un **nombre**, pas une chaîne : `BlockingStartReceiver` la relit avec
     * `getLongExtra`, qui renverrait silencieusement sa valeur par défaut si l'extra était écrit
     * comme texte. La commande serait alors rejetée et le blocage ne commencerait jamais — même
     * défaut que celui mesuré sur l'alarme du réveil à l'étape 3 (`ETAPE-03.md`).
     */
    fun blockingStart(
        sessionId: String,
        revision: Long,
    ): PendingIntentSpec =
        PendingIntentSpec(
            kind = PendingIntentSpec.Kind.BROADCAST,
            target = NiumiComponent.BLOCKING_START_RECEIVER,
            requestCode = requestCodeFor(sessionId),
            flags = IMMUTABLE_UPDATE_CURRENT,
            extras =
                mapOf(
                    "sessionId" to IntentExtraValue.Text(sessionId),
                    "revision" to IntentExtraValue.Number(revision),
                ),
        )

    private fun requestCodeFor(sessionId: String): Int = sessionId.hashCode() xor REQUEST_CODE_SALT
}
