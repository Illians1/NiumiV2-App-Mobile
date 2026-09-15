package com.niumi.system.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.SessionCoordinator
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
 * Reçoit le tic de l'alarme de secours pendant `RINGING` (SPEC_ANDROID §10.2 ; étape 20). Coquille
 * délibérément vide de décision, même patron que `SystemEventsReceiver` et `AlarmReceiver` : la
 * réconciliation seule décide si le watchdog doit être réarmé (session toujours `RINGING`) ou
 * désarmé (elle ne l'est plus, ou n'existe plus).
 *
 * **Aucun `intent-filter` dans le manifeste.** Ce receveur n'est jamais ciblé que par le
 * `PendingIntent` explicite posé par [AndroidRingingWatchdog.arm] — contrairement à
 * `SystemEventsReceiver`, il ne répond à aucun broadcast système. `directBootAware="true"` : une
 * sonnerie peut commencer avant le premier déverrouillage (prouvé sur appareil à l'étape 19), et le
 * watchdog doit pouvoir réveiller le processus dans cette fenêtre comme après.
 */
@AndroidEntryPoint
class RingingWatchdogReceiver : BroadcastReceiver() {
    @Inject
    lateinit var coordinator: SessionCoordinator

    @DefaultDispatcher
    @Inject
    lateinit var defaultDispatcher: CoroutineDispatcher

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
        scope.launch {
            try {
                withTimeout(RECONCILE_TIMEOUT_MS) { coordinator.reconcile(ReconcileReason.RINGING_WATCHDOG) }
            } catch (_: TimeoutCancellationException) {
                // Reprise par l'outbox : voir le KDoc de `SystemEventsReceiver`.
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    private companion object {
        const val RECONCILE_TIMEOUT_MS = 8_000L
    }
}
