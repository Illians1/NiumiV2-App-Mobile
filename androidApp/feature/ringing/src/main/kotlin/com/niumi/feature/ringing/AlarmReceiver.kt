package com.niumi.feature.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.ringing.ServiceCommandExtras
import com.niumi.system.session.AlarmTriggerHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Cible du `PendingIntent` explicite programmé par `AlarmScheduler` (SPEC_ANDROID §9.1, §10.1).
 * `directBootAware=true` dans le manifeste.
 *
 * Coquille délibérément vide de décision : elle lit ses extras, ouvre une fenêtre asynchrone et
 * délègue à [AlarmTriggerHandler]. Toute la logique — gardes de session et de révision,
 * surveillance de §13.1, `ALARM_FIRED` — est ainsi prouvable en JVM. **Le service de sonnerie
 * n'est plus démarré ici** : `START_RINGING` est un effet du moteur, exécuté par
 * `StartRingingExecutor` (SPEC_ANDROID §10.2, « sans écrire directement `RINGING` »).
 *
 * [goAsync] plutôt qu'un travail synchrone : la chaîne complète touche la persistance, le
 * diagnostic et les effets. La fenêtre est bornée à [DISPATCH_TIMEOUT_MS], en deçà du budget d'un
 * receiver de premier plan. Au dépassement, la coroutine est annulée et `finish()` est tout de
 * même appelé ; si l'annulation tombe après le `commit` du coordinateur, les effets restent
 * `PENDING` et la prochaine réconciliation les rejoue (SPEC_CORE_KMP §6.1). Aucun événement
 * technique n'est journalisé dans ce cas : §17 est une liste fermée et aucune de ses 26 valeurs
 * ne décrit ce fait.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var triggerHandler: AlarmTriggerHandler

    @DefaultDispatcher
    @Inject
    lateinit var defaultDispatcher: CoroutineDispatcher

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val extras =
            ServiceCommandExtras(
                sessionId = intent.getStringExtra(EXTRA_SESSION_ID),
                revision = intent.getLongExtra(EXTRA_REVISION, -1L).takeIf { intent.hasExtra(EXTRA_REVISION) },
            )
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
        scope.launch {
            try {
                withTimeout(DISPATCH_TIMEOUT_MS) { triggerHandler.handle(extras) }
            } catch (_: TimeoutCancellationException) {
                // Reprise par l'outbox : voir le KDoc de la classe.
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_REVISION = "revision"

        private const val DISPATCH_TIMEOUT_MS = 8_000L
    }
}
