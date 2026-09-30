package com.niumi.system.ringing

import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult

/**
 * Ce que le service de sonnerie journalise après avoir demandé le son (SPEC_ANDROID §17). Fonction
 * pure, hors du `Service`, pour la même raison que [RingingServiceRecovery] : le service n'est pas
 * testable en JVM.
 *
 * `RINGING_STARTED` dit que le son a **démarré**, pas que le service a été relancé. Le chien de garde
 * relance le service toutes les 60 s pendant `RINGING` (§10.2) ; `AlarmAudioEngine.start` répond
 * alors `AlreadySatisfied` et rien n'est écrit. Mesuré le 2026-09-29 sur Xiaomi 25080RABDG /
 * Android 16 : chaque relance réécrivait l'événement, soit une trentaine d'entrées pour une sonnerie
 * de 30 minutes dans un journal limité à 200. Un son qui n'a pas démarré n'est journalisé que par
 * son échec.
 */
object RingingStartJournal {
    fun eventsFor(result: OperationResult): List<TechnicalEventType> =
        when (result) {
            OperationResult.Success -> listOf(TechnicalEventType.RINGING_STARTED)
            OperationResult.AlreadySatisfied -> emptyList()
            is OperationResult.Failure -> listOf(TechnicalEventType.AUDIO_START_FAILED)
        }
}
