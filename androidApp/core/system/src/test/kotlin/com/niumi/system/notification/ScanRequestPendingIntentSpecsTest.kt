package com.niumi.system.notification

import com.google.common.truth.Truth.assertThat
import com.niumi.system.intent.IntentExtraValue
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.PendingIntentSpec
import org.junit.Test

class ScanRequestPendingIntentSpecsTest {
    private val sessionId = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"

    @Test
    fun tapSpecTargetsAlarmActivityInScanMode() {
        val spec = ScanRequestPendingIntentSpecs.tap(sessionId)

        assertThat(spec.kind).isEqualTo(PendingIntentSpec.Kind.ACTIVITY)
        assertThat(spec.target).isEqualTo(NiumiComponent.ALARM_ACTIVITY)
        assertThat(spec.extras["sessionId"]).isEqualTo(IntentExtraValue.Text(sessionId))
        assertThat(spec.extras["mode"]).isEqualTo(IntentExtraValue.Text("scan"))
    }

    /**
     * Régression : un code de requête partagé avec `AlarmPendingIntentSpecs.fullScreen` (même
     * session, même composant, `FLAG_UPDATE_CURRENT`) écraserait silencieusement les extras de
     * l'un par ceux de l'autre.
     */
    @Test
    fun tapSpecUsesADifferentRequestCodeThanTheRingingFullScreenIntent() {
        val scanSpec = ScanRequestPendingIntentSpecs.tap(sessionId)
        val ringingFullScreenRequestCode = sessionId.hashCode()

        assertThat(scanSpec.requestCode).isNotEqualTo(ringingFullScreenRequestCode)
    }
}
