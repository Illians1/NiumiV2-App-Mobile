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
    /**
     * [ringtoneFallback] (Lot 7) : `RingingSoundResolver` a remplacé une clé hors catalogue.
     * [loopFallback] (2026-10-15) : le son joue par le lecteur `MediaPlayer` de repli, avec un blanc
     * à chaque tour de boucle. Les deux suivent la même règle que `RINGING_STARTED` — écrits à une
     * vraie tentative de démarrage, jamais aux relances du chien de garde — et précèdent l'issue de
     * cette tentative, la sonnerie d'abord, le lecteur ensuite.
     */
    fun eventsFor(
        result: OperationResult,
        ringtoneFallback: Boolean = false,
        loopFallback: Boolean = false,
    ): List<TechnicalEventType> {
        val outcome =
            when (result) {
                OperationResult.Success -> TechnicalEventType.RINGING_STARTED
                OperationResult.AlreadySatisfied -> return emptyList()
                is OperationResult.Failure -> TechnicalEventType.AUDIO_START_FAILED
            }
        return listOfNotNull(
            TechnicalEventType.RINGTONE_FALLBACK.takeIf { ringtoneFallback },
            TechnicalEventType.AUDIO_LOOP_FALLBACK.takeIf { loopFallback },
            outcome,
        )
    }
}
