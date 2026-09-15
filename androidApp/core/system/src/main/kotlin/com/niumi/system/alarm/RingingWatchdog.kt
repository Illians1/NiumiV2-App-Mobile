package com.niumi.system.alarm

import com.niumi.system.common.OperationResult

/**
 * Alarme de secours pendant `RINGING` (SPEC_ANDROID §9.1, §10.2 ; étape 20). `SessionReconciler`
 * relance déjà le service de sonnerie (`resumeRinging`) quand il trouve l'état `RINGING`, mais rien
 * ne garantissait avant cette étape que le processus revienne à la vie après une mort survenue
 * pendant la sonnerie — mesuré sur appareil à l'étape 17, la plateforme ne rejoue pas toujours le
 * redémarrage `START_STICKY` du service. Ce watchdog réveille le processus périodiquement tant que
 * la session sonne, et laisse la réconciliation faire le reste.
 */
interface RingingWatchdog {
    fun arm(sessionId: String): OperationResult

    fun disarm(sessionId: String): OperationResult
}
