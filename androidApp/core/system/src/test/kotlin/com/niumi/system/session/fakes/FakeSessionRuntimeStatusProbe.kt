package com.niumi.system.session.fakes

import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.session.SessionRuntimeStatus
import com.niumi.system.session.SessionRuntimeStatusProbe

/**
 * Sonde de test pour [com.niumi.system.session.SessionRuntimeReconciler] (étape 20).
 * `alarmScheduled` délègue au vrai [AlarmScheduler] du harnais plutôt qu'à un champ figé : une
 * réparation qui appelle `schedule()` doit se voir à la re-sonde suivante, exactement comme sur
 * appareil. Les quatre champs qui n'appartiennent pas à `SessionRuntimeReconciler` restent sains
 * par défaut : ce sont ceux de `SessionReadinessMonitor` (§13.1), couverts ailleurs.
 */
class FakeSessionRuntimeStatusProbe(
    private val alarmScheduler: AlarmScheduler,
) : SessionRuntimeStatusProbe {
    var nfcReady: Boolean = true

    override fun probe(sessionId: String): SessionRuntimeStatus =
        SessionRuntimeStatus(
            alarmScheduled = alarmScheduler.isScheduled(sessionId),
            accessibilityReady = true,
            notificationReady = true,
            fullScreenReady = true,
            nfcReady = nfcReady,
            audioReady = true,
        )
}
