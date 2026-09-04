package com.niumi.feature.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.ringing.RingingController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Cible du `PendingIntent` explicite programmé par `AlarmScheduler` (SPEC_ANDROID §9.1,
 * §10.1). Ne fait aucun travail long : valide les extras et délègue immédiatement à
 * [RingingController]. `directBootAware=true` dans le manifeste.
 *
 * Écart assumé de cette étape (voir `ETAPE-03.md`) : SPEC_ANDROID §10.1 demande que ce receiver
 * transmette `ALARM_FIRED` à `NiumiCoreFacade`, qui n'existe pas encore (Phase C, étape 17).
 * Il appelle directement `RingingController` en attendant.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var ringingController: RingingController

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val extras =
            ServiceCommandExtras(
                sessionId = intent.getStringExtra(EXTRA_SESSION_ID),
                revision = intent.getLongExtra(EXTRA_REVISION, -1L).takeIf { intent.hasExtra(EXTRA_REVISION) },
            )
        when (val command = ServiceCommand.from(extras)) {
            is ServiceCommand.Valid -> {
                technicalEventLog.log(TechnicalEventType.ALARM_RECEIVED, sessionId = command.sessionId)
                ringingController.startRinging(command.sessionId, command.revision)
            }

            is ServiceCommand.Invalid -> {
                technicalEventLog.log(TechnicalEventType.ALARM_RECEIVED, sessionId = null)
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_REVISION = "revision"
    }
}
