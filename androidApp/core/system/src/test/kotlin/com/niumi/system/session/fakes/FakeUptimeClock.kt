package com.niumi.system.session.fakes

import com.niumi.system.common.UptimeClock

/**
 * Démarrage lointain par défaut : un test qui ne parle pas du démarrage ne doit pas tomber dans
 * la fenêtre où le NFC n'est pas jugé (`SessionNfcEvaluability`).
 */
class FakeUptimeClock(
    var elapsedMillis: Long = ONE_HOUR_MILLIS,
) : UptimeClock {
    override fun elapsedSinceBootMillis(): Long = elapsedMillis

    companion object {
        const val ONE_HOUR_MILLIS: Long = 60 * 60 * 1_000L
    }
}
