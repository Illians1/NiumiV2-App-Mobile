package com.niumi.feature.session.blocking.guard

import android.content.Context
import android.content.Intent
import com.niumi.system.blocking.guard.BlockingGuardCommand
import com.niumi.system.blocking.guard.BlockingGuardController
import com.niumi.system.common.OperationResult

/**
 * Démarre, met à jour et arrête [BlockingGuardService] (SPEC_ANDROID §12.5).
 *
 * Android 12+ refuse de démarrer un service de premier plan depuis l'arrière-plan, sauf exemption :
 * alarme exacte (début différé), `BOOT_COMPLETED` et ses voisins, changement d'heure, et surtout
 * l'exemption d'énergie que le diagnostic exige. Un refus lève
 * `ForegroundServiceStartNotAllowedException`, sous-classe d'`IllegalStateException` (API 31) ;
 * il devient ici un [OperationResult.Failure], jamais une panne du processus.
 */
class AndroidBlockingGuardController(
    private val context: Context,
) : BlockingGuardController {
    override fun keep(command: BlockingGuardCommand): OperationResult =
        try {
            context.startForegroundService(
                Intent(context, BlockingGuardService::class.java)
                    .putExtra(BlockingGuardService.EXTRA_SESSION_ID, command.sessionId)
                    .putExtra(BlockingGuardService.EXTRA_PHASE, command.phase.name)
                    .putExtra(BlockingGuardService.EXTRA_WAKE_AT, command.wakeAtEpochMillis)
                    .putExtra(BlockingGuardService.EXTRA_ZONE_ID, command.zoneId)
                    .apply {
                        // Absent pour un blocage immédiat : l'extra n'est posé que s'il existe, le
                        // service relit son absence comme « pas d'heure ».
                        command.blockingStartsAtEpochMillis?.let {
                            putExtra(
                                BlockingGuardService.EXTRA_BLOCKING_AT,
                                it,
                            )
                        }
                    },
            )
            OperationResult.Success
        } catch (e: IllegalStateException) {
            OperationResult.Failure(START_NOT_ALLOWED, e)
        }

    override fun release(): OperationResult =
        if (context.stopService(Intent(context, BlockingGuardService::class.java))) {
            OperationResult.Success
        } else {
            OperationResult.AlreadySatisfied
        }

    private companion object {
        const val START_NOT_ALLOWED = "ANDROID_FGS_START_NOT_ALLOWED"
    }
}
