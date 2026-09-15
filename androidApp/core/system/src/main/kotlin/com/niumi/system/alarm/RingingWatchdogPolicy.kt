package com.niumi.system.alarm

import com.niumi.core.interop.SessionStateDto

/** Ce que [RingingWatchdogSpecs] fait de la session : armer, ou désarmer. */
sealed interface WatchdogAction {
    data object Arm : WatchdogAction

    data object Disarm : WatchdogAction
}

/**
 * Décide si l'alarme de secours doit être armée ou désarmée pour un état donné (SPEC_ANDROID
 * §10.2). `when` exhaustif sur l'énumération : un état ajouté sans être classé ici casse la
 * compilation plutôt que de laisser le watchdog dans un état indéterminé.
 */
object RingingWatchdogPolicy {
    fun decide(state: SessionStateDto): WatchdogAction =
        when (state) {
            SessionStateDto.RINGING -> WatchdogAction.Arm

            SessionStateDto.PREPARING,
            SessionStateDto.ARMED,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            SessionStateDto.RELEASING,
            SessionStateDto.COMPLETED,
            SessionStateDto.CANCELLED,
            SessionStateDto.FAILED,
            -> WatchdogAction.Disarm
        }
}
