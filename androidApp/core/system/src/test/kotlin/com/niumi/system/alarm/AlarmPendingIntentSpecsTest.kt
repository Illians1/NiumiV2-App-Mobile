package com.niumi.system.alarm

import android.app.PendingIntent
import com.google.common.truth.Truth.assertThat
import com.niumi.system.intent.IntentExtraValue
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.PendingIntentSpec
import org.junit.Test

/**
 * SPEC_ANDROID §9.1 : les trois `PendingIntent` liés à une alarme sont explicites, immuables
 * et portent un code de requête stable pour une session donnée. Ces specs sont pures : aucune
 * classe Android réelle n'est instanciée, seules les constantes entières de `PendingIntent`
 * sont lues (inlinées à la compilation, donc lisibles dans le jar stub des tests JVM).
 */
class AlarmPendingIntentSpecsTest {
    private val sessionId = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"

    @Test
    fun alarmSpecTargetsAlarmReceiverAsExplicitBroadcast() {
        val spec = AlarmPendingIntentSpecs.alarm(sessionId, revision = 1L)

        assertThat(spec.kind).isEqualTo(PendingIntentSpec.Kind.BROADCAST)
        assertThat(spec.target).isEqualTo(NiumiComponent.ALARM_RECEIVER)
        assertThat(spec.flags and PendingIntent.FLAG_IMMUTABLE).isNotEqualTo(0)
        assertThat(spec.flags and PendingIntent.FLAG_UPDATE_CURRENT).isNotEqualTo(0)
        assertThat(spec.extras["sessionId"]).isEqualTo(IntentExtraValue.Text(sessionId))
        assertThat(spec.extras["revision"]).isEqualTo(IntentExtraValue.Number(1L))
    }

    /**
     * Régression : `revision` avait été écrite comme chaîne alors que `AlarmReceiver` la relit
     * avec `getLongExtra`. Aucune exception, aucun avertissement — la valeur par défaut était
     * renvoyée, la commande rejetée, et l'alarme restait muette. Ce test verrouille le type,
     * pas seulement la valeur (voir `ETAPE-03.md`).
     */
    @Test
    fun revisionExtraIsNumericNotText() {
        val spec = AlarmPendingIntentSpecs.alarm(sessionId, revision = 42L)

        assertThat(spec.extras["revision"]).isInstanceOf(IntentExtraValue.Number::class.java)
        assertThat((spec.extras["revision"] as IntentExtraValue.Number).value).isEqualTo(42L)
    }

    @Test
    fun showSpecTargetsMainActivity() {
        val spec = AlarmPendingIntentSpecs.show(sessionId)

        assertThat(spec.kind).isEqualTo(PendingIntentSpec.Kind.ACTIVITY)
        assertThat(spec.target).isEqualTo(NiumiComponent.MAIN_ACTIVITY)
    }

    @Test
    fun fullScreenSpecTargetsAlarmActivity() {
        val spec = AlarmPendingIntentSpecs.fullScreen(sessionId)

        assertThat(spec.kind).isEqualTo(PendingIntentSpec.Kind.ACTIVITY)
        assertThat(spec.target).isEqualTo(NiumiComponent.ALARM_ACTIVITY)
    }

    @Test
    fun requestCodeIsStableForTheSameSessionAcrossCalls() {
        val first = AlarmPendingIntentSpecs.alarm(sessionId, revision = 1L)
        val second = AlarmPendingIntentSpecs.alarm(sessionId, revision = 2L)

        assertThat(first.requestCode).isEqualTo(second.requestCode)
    }

    @Test
    fun requestCodeIsSharedAcrossTheThreeSpecsOfTheSameSession() {
        val alarm = AlarmPendingIntentSpecs.alarm(sessionId, revision = 1L)
        val show = AlarmPendingIntentSpecs.show(sessionId)
        val fullScreen = AlarmPendingIntentSpecs.fullScreen(sessionId)

        assertThat(alarm.requestCode).isEqualTo(sessionId.hashCode())
        assertThat(show.requestCode).isEqualTo(sessionId.hashCode())
        assertThat(fullScreen.requestCode).isEqualTo(sessionId.hashCode())
    }

    @Test
    fun differentSessionsProduceDifferentRequestCodes() {
        val other = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"

        val first = AlarmPendingIntentSpecs.alarm(sessionId, revision = 1L)
        val second = AlarmPendingIntentSpecs.alarm(other, revision = 1L)

        assertThat(first.requestCode).isNotEqualTo(second.requestCode)
    }
}
