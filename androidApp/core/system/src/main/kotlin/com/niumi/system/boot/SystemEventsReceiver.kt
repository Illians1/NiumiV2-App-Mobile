package com.niumi.system.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niumi.system.common.DefaultDispatcher
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
 * Traduit les broadcasts système en réconciliations (SPEC_ANDROID §9.3). `directBootAware=true` et
 * `exported="false"` dans le manifeste de `:core:system`.
 *
 * Coquille délibérément vide de décision, comme `AlarmReceiver` : elle lit l'action, la traduit par
 * [SystemEventReasons] et délègue au coordinateur. Toute la logique — reprogrammation au même
 * instant, politique de retard, incident de changement d'heure, fusion Direct Boot → Room — vit dans
 * `SessionReconciler` et `DirectBootMerger`, et reste ainsi prouvable en JVM.
 *
 * Cinq actions seulement. **`USER_UNLOCKED` n'est pas déclarable ici** : Android ne le délivre qu'aux
 * receivers enregistrés à chaud, jamais à un receiver de manifeste. Il est enregistré par
 * [SystemEventsRegistrar] depuis l'application. Les cinq actions déclarées sont, elles, bien
 * délivrées à un receiver de manifeste : `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `TIME_SET` et
 * `TIMEZONE_CHANGED` figurent dans la liste officielle des exceptions aux restrictions de
 * broadcasts implicites, et `MY_PACKAGE_REPLACED` est explicitement adressé au paquet lui-même.
 *
 * [goAsync] plutôt qu'un travail synchrone : la chaîne complète touche la persistance, le
 * diagnostic et les effets. La fenêtre est bornée à [RECONCILE_TIMEOUT_MS], en deçà du budget d'un
 * receiver de premier plan. Au dépassement, la coroutine est annulée et `finish()` est tout de même
 * appelé ; une décision déjà persistée laisse ses effets `PENDING` et la prochaine réconciliation
 * les rejoue (SPEC_CORE_KMP §6.1). Aucun événement technique n'est journalisé dans ce cas : §17 est
 * une liste fermée et aucune de ses 26 valeurs ne décrit ce fait.
 */
@AndroidEntryPoint
class SystemEventsReceiver : BroadcastReceiver() {
    @Inject
    lateinit var coordinator: SessionCoordinator

    @DefaultDispatcher
    @Inject
    lateinit var defaultDispatcher: CoroutineDispatcher

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val reason = SystemEventReasons.reasonOf(intent.action) ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
        scope.launch {
            try {
                withTimeout(RECONCILE_TIMEOUT_MS) { coordinator.reconcile(reason) }
            } catch (_: TimeoutCancellationException) {
                // Reprise par l'outbox : voir le KDoc de la classe.
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
