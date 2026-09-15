package com.niumi.system.alarm

import android.app.PendingIntent
import com.google.common.truth.Truth.assertThat
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.PendingIntentSpec
import org.junit.Test

/**
 * SPEC_ANDROID §9.1, §10.2 (étape 20) : le `PendingIntent` de l'alarme de secours de `RINGING`
 * doit toujours pouvoir être distingué de celui du réveil, pour la même session. Même style de
 * preuve qu'`AlarmPendingIntentSpecsTest` : ces specs sont pures, aucune classe Android réelle
 * n'est instanciée.
 */
class RingingWatchdogSpecsTest {
    private val sessionId = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"

    @Test
    fun theWatchdogTargetsItsOwnReceiverAsAnExplicitBroadcast() {
        val spec = RingingWatchdogSpecs.pendingIntent(sessionId)

        assertThat(spec.kind).isEqualTo(PendingIntentSpec.Kind.BROADCAST)
        assertThat(spec.target).isEqualTo(NiumiComponent.RINGING_WATCHDOG_RECEIVER)
        assertThat(spec.flags and PendingIntent.FLAG_IMMUTABLE).isNotEqualTo(0)
        assertThat(spec.flags and PendingIntent.FLAG_UPDATE_CURRENT).isNotEqualTo(0)
    }

    /**
     * Le point qui compte : `AlarmManager.cancel()` sur l'une des deux alarmes ne doit jamais
     * pouvoir atteindre l'autre. Comparé au code de requête public d'`AlarmPendingIntentSpecs`,
     * pas à un détail d'implémentation interne.
     */
    @Test
    fun theWatchdogRequestCodeNeverCollidesWithTheWakeAlarmForTheSameSession() {
        val watchdog = RingingWatchdogSpecs.pendingIntent(sessionId)
        val wakeAlarm = AlarmPendingIntentSpecs.alarm(sessionId, revision = 1L)

        assertThat(watchdog.requestCode).isNotEqualTo(wakeAlarm.requestCode)
    }

    @Test
    fun theRequestCodeIsStableForAGivenSession() {
        val first = RingingWatchdogSpecs.pendingIntent(sessionId)
        val second = RingingWatchdogSpecs.pendingIntent(sessionId)

        assertThat(first.requestCode).isEqualTo(second.requestCode)
    }

    @Test
    fun differentSessionsProduceDifferentRequestCodes() {
        val other = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"

        val first = RingingWatchdogSpecs.pendingIntent(sessionId)
        val second = RingingWatchdogSpecs.pendingIntent(other)

        assertThat(first.requestCode).isNotEqualTo(second.requestCode)
    }

    @Test
    fun thePeriodIsSixtySeconds() {
        assertThat(RingingWatchdogSpecs.PERIOD_MS).isEqualTo(60_000L)
    }

    @Test
    fun nextTriggerIsExactlyOnePeriodAfterNow() {
        assertThat(RingingWatchdogSpecs.nextTriggerAt(1_000L)).isEqualTo(1_000L + RingingWatchdogSpecs.PERIOD_MS)
    }
}
