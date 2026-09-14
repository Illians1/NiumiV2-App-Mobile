package com.niumi.system.readiness

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.SESSION_SCAN_STATES
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Déclencheurs de la surveillance de SPEC_ANDROID §13.1 **hors réconciliation** : passage de
 * l'application au premier plan, et changement public du filtre d'interruption. La
 * réconciliation, elle, appelle [SessionReadinessMonitor] directement depuis
 * `SessionReconciler`, à l'intérieur du verrou du coordinateur.
 *
 * **Limite à ne pas masquer (§13.1).** Un receveur enregistré à chaud meurt avec le processus,
 * et Android ne diffuse aucun broadcast public pour plusieurs des réglages surveillés, à
 * commencer par le volume. Un réglage modifié pendant que Niumi est mort n'est donc découvert
 * qu'au réveil suivant du processus. Aucune promesse de surveillance continue n'est faite.
 *
 * Le service d'accessibilité n'est jamais employé comme sentinelle, bien qu'il soit le seul
 * composant vivant en continu : son usage déclaré à Google Play est le seul blocage (§12.3).
 */
class SessionReadinessWatcher(
    private val publisher: SessionSnapshotPublisher,
    private val monitor: SessionReadinessMonitor,
    private val coordinator: SessionCoordinator,
    dispatcher: CoroutineDispatcher,
) : ForegroundReadinessTrigger {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val interruptionFilterReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) evaluateAsync()
            }
        }

    /**
     * Aucune session publiée signifie qu'aucune n'est active **dans ce processus** : la
     * surveillance n'a alors pas d'objet. La réconciliation, qui lit la persistance, reste le
     * chemin qui découvre une session après un redémarrage du processus.
     *
     * **Une session qui attend un scan déclenche en plus une réconciliation (§10.5, étape 19).**
     * Elle republie la notification d'attente de scan, seul rappel visible une fois l'écran de
     * réveil fermé. Décidé sur une mesure, non au jugé : après un balayage de cette notification,
     * elle est restée absente 523 s sans revenir, et aucune des autres raisons de réconciliation
     * ne survient pendant qu'on se sert du téléphone — ni le passage au premier plan, ni un
     * déverrouillage d'écran ordinaire (`ACTION_USER_UNLOCKED` n'est émis qu'au premier
     * déverrouillage après démarrage). Mesuré le 2026-09-14 sur Xiaomi 25080RABDG / Android 16.
     *
     * La surveillance de §13.1 est appelée en premier et n'est pas remplacée : `reconcile` ne
     * rejoue le diagnostic que sur une session `ARMED`, alors que le service d'accessibilité reste
     * surveillé dans tous les états non finaux.
     */
    suspend fun evaluate() {
        val snapshot = publisher.snapshot.value ?: return
        monitor.evaluate(snapshot) { event -> coordinator.dispatch(event) }
        if (snapshot.state in SESSION_SCAN_STATES) {
            coordinator.reconcile(ReconcileReason.FOREGROUND_AWAITING_SCAN)
        }
    }

    override fun evaluateAsync() {
        scope.launch { evaluate() }
    }

    /** Enregistré au démarrage du processus par `NiumiApplication` (§13.1). */
    fun registerInterruptionFilterReceiver(context: Context) {
        context.registerReceiver(
            interruptionFilterReceiver,
            IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
    }
}
