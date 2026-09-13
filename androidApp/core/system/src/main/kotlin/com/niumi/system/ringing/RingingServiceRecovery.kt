package com.niumi.system.ringing

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.LoadResult

/** Ce que le service de sonnerie doit faire quand il redémarre sans commande. */
sealed interface RingingRecovery {
    /** Reprendre le son : la session sonnait quand le processus est mort (SPEC_ANDROID §10.2). */
    data class ResumeRinging(
        val sessionId: String,
        val revision: Long,
    ) : RingingRecovery

    /** Session encore armée : réconcilier (`SERVICE_RECREATED`) puis s'arrêter. */
    data object ReconcileAndStop : RingingRecovery

    /** S'arrêter sans rien décider : aucun de ces états n'attend du son. */
    data object StopWithoutTouchingState : RingingRecovery

    /** Snapshot illisible : ne pas sonner, ne rien effacer, réconcilier puis s'arrêter. */
    data object StopOnCorruptedSnapshot : RingingRecovery
}

/**
 * Décide de la reprise du service de sonnerie à partir du seul snapshot persisté (SPEC_ANDROID
 * §10.2). Fonction pure, hors du `Service` : `onStartCommand` n'est pas testable en JVM, cette
 * table de correspondance l'est entièrement.
 *
 * Le cas `Unreadable` n'était couvert ni par le plan ni par §10.2, et les deux issues sont
 * mauvaises à moitié : sonner sur un snapshot illisible expose l'utilisateur à une alarme **sans
 * bouton d'arrêt** (§10.2) pour une session peut-être terminée ; s'arrêter risque de ne pas
 * réveiller. Arbitrage retenu : ne pas sonner, ne rien effacer, conserver le blocage (§18) et
 * laisser la réconciliation consigner `SnapshotCorrupted`.
 */
object RingingServiceRecovery {
    fun decide(loaded: LoadResult): RingingRecovery =
        when (loaded) {
            is LoadResult.Unreadable -> {
                RingingRecovery.StopOnCorruptedSnapshot
            }

            is LoadResult.Absent -> {
                RingingRecovery.StopWithoutTouchingState
            }

            is LoadResult.Present -> {
                when (loaded.snapshot.state) {
                    SessionStateDto.RINGING -> {
                        RingingRecovery.ResumeRinging(loaded.snapshot.sessionId, loaded.snapshot.revision)
                    }

                    SessionStateDto.ARMED -> {
                        RingingRecovery.ReconcileAndStop
                    }

                    SessionStateDto.PREPARING,
                    SessionStateDto.AWAITING_NFC,
                    SessionStateDto.TRIGGERED_AWAITING_NFC,
                    SessionStateDto.RELEASING,
                    SessionStateDto.COMPLETED,
                    SessionStateDto.CANCELLED,
                    SessionStateDto.FAILED,
                    -> {
                        RingingRecovery.StopWithoutTouchingState
                    }
                }
            }
        }
}
