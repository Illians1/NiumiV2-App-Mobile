package com.niumi.system.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.ringing.ServiceCommandExtras
import com.niumi.system.session.BlockingStartHandler
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
 * Cible du `PendingIntent` de début du blocage différé (SPEC_ANDROID §12.4, §14 ; Lot 6).
 *
 * Coquille sans décision, comme `AlarmReceiver` : il lit ses extras, ouvre `goAsync()` et délègue à
 * [BlockingStartHandler], ce qui rend toute la chaîne prouvable en JVM. Les constantes d'extras sont
 * redéfinies ici plutôt qu'empruntées à `AlarmReceiver` : celui-ci vit dans `:feature:ringing`, que
 * `:core:system` ne voit pas (SPEC_ANDROID §6). Leurs noms doivent rester ceux qu'écrit
 * `BlockingStartPendingIntentSpecs`.
 *
 * Fenêtre bornée à 8 s comme les deux autres receveurs. Au dépassement, la coroutine est annulée : si
 * l'annulation tombe après le `commit` du coordinateur, les effets restent `PENDING` et la prochaine
 * réconciliation les rejoue (SPEC_CORE_KMP §6.1).
 */
@AndroidEntryPoint
class BlockingStartReceiver : BroadcastReceiver() {
    @Inject
    lateinit var handler: BlockingStartHandler

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
                withTimeout(DISPATCH_TIMEOUT_MS) { handler.handle(extras) }
            } catch (_: TimeoutCancellationException) {
                // Effets laissés `PENDING`, rejoués à la prochaine réconciliation (SPEC_CORE_KMP §6.1).
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    private companion object {
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_REVISION = "revision"
        const val DISPATCH_TIMEOUT_MS = 8_000L
    }
}
