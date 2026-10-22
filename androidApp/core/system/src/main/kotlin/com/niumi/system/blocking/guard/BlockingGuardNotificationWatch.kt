package com.niumi.system.blocking.guard

import com.niumi.system.notification.RingingNotificationAction
import com.niumi.system.notification.RingingNotificationWatch

/**
 * Ce que la garde fait d'une notification écartée (SPEC_ANDROID §12.5, décision de Mehdi du
 * 2026-10-22, qui revient sur « jamais ramenée » du 2026-10-19).
 *
 * - **Avant l'alarme**, elle ne fait qu'informer : écartée, elle le reste.
 * - **Pendant la sonnerie**, la notification est partagée avec `AlarmRingingService`, qui la ramène
 *   déjà en dix secondes au plus (§10.2) : la garde n'en fait pas une seconde surveillance.
 * - **En attente de scan**, elle est le rappel du seul geste qui termine la session : la garde la
 *   ramène. Sans elle, une notification de scan écartée ne revenait qu'à la réouverture de Niumi
 *   (§10.5, mesuré à l'étape 19).
 */
object BlockingGuardNotificationWatch {
    /** Même cadence que la sonnerie : au pire dix secondes sans rappel visible. */
    const val CHECK_INTERVAL_MS = RingingNotificationWatch.CHECK_INTERVAL_MS

    fun isWatched(phase: BlockingGuardPhase): Boolean = phase == BlockingGuardPhase.AWAITING_SCAN

    fun decide(
        phase: BlockingGuardPhase,
        isPosted: Boolean,
    ): RingingNotificationAction =
        if (isWatched(phase) && !isPosted) RingingNotificationAction.REPUBLISH else RingingNotificationAction.NONE
}
