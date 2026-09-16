package com.niumi.system.alarm

import com.niumi.system.common.OperationResult

/**
 * Alarme exacte du début d'un blocage différé (SPEC_ANDROID §9.1 seconde dérogation, §12.4 ; Lot 6).
 * Distincte du réveil et du watchdog : son `PendingIntent` cible `BlockingStartReceiver` avec un code
 * de requête salé, si bien qu'aucun `cancel()` ne peut atteindre l'une en visant l'autre.
 *
 * Elle n'est programmée que pour un blocage différé. Une session à blocage immédiat applique le
 * blocage dès `ACTIVATION_REQUESTED` et n'a donc jamais d'alarme de début (SPEC_CORE_KMP §6).
 */
interface BlockingStartScheduler {
    /** Programme le début au même instant contractuel ; idempotent (`FLAG_UPDATE_CURRENT`). */
    fun schedule(
        sessionId: String,
        revision: Long,
        startsAtEpochMillis: Long,
    ): OperationResult

    /** Annulation idempotente : `AlreadySatisfied` quand aucun `PendingIntent` n'existe. */
    fun cancel(sessionId: String): OperationResult

    fun isScheduled(sessionId: String): Boolean
}
