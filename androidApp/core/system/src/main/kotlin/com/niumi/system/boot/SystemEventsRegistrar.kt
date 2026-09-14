package com.niumi.system.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.SessionCoordinator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Enregistrement à chaud de `ACTION_USER_UNLOCKED` (SPEC_ANDROID §9.3).
 *
 * **Écart de spec assumé, imposé par la plateforme.** §9.3 liste `USER_UNLOCKED` parmi les filtres
 * du `SystemEventsReceiver` déclaré au manifeste. Android ne délivre jamais ce broadcast à un
 * receiver de manifeste : la documentation Direct Boot demande d'« enregistrer un
 * `BroadcastReceiver` depuis un composant qui tourne ». Déclaré au manifeste, le filtre serait mort
 * et la fusion Direct Boot → Room n'aurait jamais lieu par ce chemin.
 *
 * **Conséquence, et son filet.** Un enregistrement à chaud n'existe que si le processus est vivant
 * au moment du déverrouillage. C'est le cas qui compte — un composant `directBootAware` a réveillé
 * Niumi parce que le réveil est passé —, mais pas le seul possible. Le coordinateur tente donc aussi
 * la fusion sur `BOOT` et `PROCESS_START` ; elle est idempotente et ne coûte qu'une lecture de
 * fichier quand il n'y a rien à absorber. Voir [DirectBootMerger].
 *
 * `RECEIVER_NOT_EXPORTED` : `USER_UNLOCKED` est un broadcast protégé du système, aucune application
 * tierce ne peut l'émettre, et rien ne justifie d'ouvrir ce receveur. Même patron que
 * `SessionReadinessWatcher.registerInterruptionFilterReceiver`.
 *
 * Le receveur meurt avec le processus, ce qui est exactement la durée de vie voulue : il n'a de sens
 * que pour le processus qui a traversé la fenêtre Direct Boot.
 */
class SystemEventsRegistrar(
    private val coordinator: SessionCoordinator,
    dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val userUnlockedReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == Intent.ACTION_USER_UNLOCKED) reconcileAsync()
            }
        }

    /**
     * Même patron qu'`AlarmReceiver` et `SessionReadinessWatcher` : le receveur ne décide de rien,
     * il lance la passe. Pas de `goAsync()` ici — un receveur enregistré à chaud vit dans un
     * processus déjà en vie, que ce broadcast ne réveille pas.
     */
    private fun reconcileAsync() {
        scope.launch { coordinator.reconcile(ReconcileReason.USER_UNLOCKED) }
    }

    /** Appelé au démarrage du processus par `NiumiApplication`. */
    fun registerUserUnlockedReceiver(context: Context) {
        context.registerReceiver(
            userUnlockedReceiver,
            IntentFilter(Intent.ACTION_USER_UNLOCKED),
            Context.RECEIVER_NOT_EXPORTED,
        )
    }
}
