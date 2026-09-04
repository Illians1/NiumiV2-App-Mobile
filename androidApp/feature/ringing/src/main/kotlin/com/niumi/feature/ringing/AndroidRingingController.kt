package com.niumi.feature.ringing

import android.content.Context
import android.content.Intent
import com.niumi.system.common.OperationResult
import com.niumi.system.ringing.RingingController

/** Démarre/arrête `AlarmRingingService` via une commande explicite validée. */
class AndroidRingingController(
    private val context: Context,
) : RingingController {
    override fun startRinging(
        sessionId: String,
        revision: Long,
    ): OperationResult {
        val intent =
            Intent(context, AlarmRingingService::class.java)
                .putExtra(AlarmReceiver.EXTRA_SESSION_ID, sessionId)
                .putExtra(AlarmReceiver.EXTRA_REVISION, revision)
        // Le déclenchement d'une alarme exacte demandée par l'utilisateur autorise le
        // démarrage du service au premier plan depuis l'arrière-plan (SPEC_ANDROID §10.1).
        // `minSdk 29` : `startForegroundService` est toujours disponible, sans branche héritée.
        context.startForegroundService(intent)
        return OperationResult.Success
    }

    override fun stopRinging(sessionId: String): OperationResult {
        context.stopService(Intent(context, AlarmRingingService::class.java))
        return OperationResult.Success
    }
}
