package com.niumi.system.alarm

import android.app.PendingIntent
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.PendingIntentSpec

/**
 * `PendingIntent` de l'alarme de secours pendant `RINGING` (SPEC_ANDROID §9.1, §10.2 ; étape 20).
 * Le code de requête est volontairement **distinct** de celui d'[AlarmPendingIntentSpecs] pour une
 * même session : les deux alarmes doivent apparaître séparément dans `dumpsys alarm`, et
 * `AlarmManager.cancel()` ne doit jamais pouvoir atteindre l'une en visant l'autre.
 */
object RingingWatchdogSpecs {
    /**
     * SPEC_ANDROID §10.2 : une seule alarme de secours à la fois, réarmée en chaîne par son propre
     * receveur tant que la session sonne.
     */
    const val PERIOD_MS: Long = 60_000L

    private const val IMMUTABLE_UPDATE_CURRENT =
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    // Octets ASCII de "WATC", arbitraire mais stable : évite toute collision avec
    // `AlarmPendingIntentSpecs`, qui utilise `sessionId.hashCode()` nu pour ses trois
    // `PendingIntent`.
    private const val REQUEST_CODE_SALT = 0x57415443

    fun pendingIntent(sessionId: String): PendingIntentSpec =
        PendingIntentSpec(
            kind = PendingIntentSpec.Kind.BROADCAST,
            target = NiumiComponent.RINGING_WATCHDOG_RECEIVER,
            requestCode = requestCodeFor(sessionId),
            flags = IMMUTABLE_UPDATE_CURRENT,
        )

    fun nextTriggerAt(nowEpochMillis: Long): Long = nowEpochMillis + PERIOD_MS

    private fun requestCodeFor(sessionId: String): Int = sessionId.hashCode() xor REQUEST_CODE_SALT
}
