package com.niumi.system.readiness.fakes

import com.niumi.system.readiness.ReadinessRecheck

/** Compte les demandes de re-contrôle : leur exécution différée est prouvée par `CoroutineReadinessRecheckTest`. */
class RecordingReadinessRecheck : ReadinessRecheck {
    var requests: Int = 0
        private set

    override fun recheckAfterSettling() {
        requests++
    }
}
