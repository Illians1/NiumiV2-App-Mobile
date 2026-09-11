package com.niumi.system.notification

import android.app.PendingIntent
import com.niumi.system.intent.IntentExtraValue
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.PendingIntentSpec

/**
 * `PendingIntent` du tap sur la notification d'attente de scan (SPEC_ANDROID §10.5 : ouvre
 * `AlarmActivity` en mode scan). Code de requête distinct de `AlarmPendingIntentSpecs.fullScreen`
 * (même session, même composant cible) : les deux notifications ne portent pas le même contrat
 * d'extras, elles ne doivent jamais partager l'identité d'un `PendingIntent`.
 */
object ScanRequestPendingIntentSpecs {
    private const val IMMUTABLE_UPDATE_CURRENT = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    private const val MODE_SCAN = "scan"

    fun tap(sessionId: String): PendingIntentSpec =
        PendingIntentSpec(
            kind = PendingIntentSpec.Kind.ACTIVITY,
            target = NiumiComponent.ALARM_ACTIVITY,
            requestCode = requestCodeFor(sessionId),
            flags = IMMUTABLE_UPDATE_CURRENT,
            extras =
                mapOf(
                    "sessionId" to IntentExtraValue.Text(sessionId),
                    "mode" to IntentExtraValue.Text(MODE_SCAN),
                ),
        )

    private fun requestCodeFor(sessionId: String): Int = (sessionId + ":scan").hashCode()
}
