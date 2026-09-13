package com.niumi.system.session

import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.readiness.SessionReadinessMonitor
import com.niumi.system.ringing.ServiceCommand
import com.niumi.system.ringing.ServiceCommandExtras

/**
 * Ce qu'a fait le déclenchement. Les cinq valeurs hors [Dispatched] sont des refus : aucun
 * événement n'a atteint le moteur, et l'état de la session est inchangé.
 */
sealed interface AlarmTriggerOutcome {
    data class Dispatched(
        val result: DispatchResult,
    ) : AlarmTriggerOutcome

    data object InvalidCommand : AlarmTriggerOutcome

    data object NoSession : AlarmTriggerOutcome

    data object UnknownSession : AlarmTriggerOutcome

    data object RevisionAhead : AlarmTriggerOutcome

    data object UnreadableSnapshot : AlarmTriggerOutcome
}

/**
 * Traduit le déclenchement de l'alarme exacte en `ALARM_FIRED` (SPEC_ANDROID §10.1 ; SPEC_CORE_KMP
 * §6, §11.1). `AlarmReceiver` ne fait que lire ses extras et déléguer ici : toute la décision est
 * ainsi prouvable en JVM, sans `Intent` ni composant Android.
 *
 * **Garde de révision : monotonie, pas égalité.** L'extra du `PendingIntent` est figé au moment de
 * `SCHEDULE_ALARM`, et la révision du snapshot avance sans le réécrire : `INCIDENT_REPORTED`
 * incrémente la révision et ne produit aucun `SCHEDULE_ALARM` (SPEC_CORE_KMP §6), tandis que le
 * réconciliateur ne reprogramme que si l'alarme a disparu. Un extra en retard est donc le cas
 * **courant** dès que la surveillance de §13.1 a signalé quoi que ce soit, et le refuser rendrait
 * le réveil muet. Seule une révision **supérieure** au snapshot est refusée : elle décrit une
 * persistance en retard sur ce qui a été programmé (snapshot Direct Boot périmé, restauration),
 * que §16 impose de ne pas suivre aveuglément.
 *
 * **La surveillance de §13.1 s'exécute avant le dispatch**, et pas après : une fois `ALARM_FIRED`
 * appliqué, l'état est `RINGING` et `SessionReadinessMonitor` ne surveille plus que le service
 * d'accessibilité — l'incident `ANDROID_ALARM_MUTED_BY_DND` exigé par §13 ne serait jamais créé.
 * Elle est réutilisée telle quelle plutôt que doublée par une seconde lecture du filtre
 * d'interruption : le moniteur porte déjà la table code/gravité, la déduplication par session et
 * la notification d'avertissement, et une seconde voie recréerait les doublons corrigés à
 * l'étape 16. Comme cette passe peut dispatcher `INCIDENT_REPORTED`, **le snapshot est relu
 * ensuite** : bâtir `alarmFired` sur le snapshot d'avant produirait `STALE_REVISION`.
 *
 * [coordinator] est appelé par `dispatch`, jamais par `reconcile` : le handler s'exécute hors du
 * `Mutex`, et une réconciliation en `FIRE_NOW` hors `BEFORE_SCAN` **reprogrammerait** l'alarme au
 * lieu de la déclencher.
 */
class AlarmTriggerHandler(
    private val gateway: SessionPersistenceGateway,
    private val coordinator: SessionCoordinator,
    private val eventFactory: SessionEventFactory,
    private val readinessMonitor: SessionReadinessMonitor,
    private val technicalEventLog: TechnicalEventLog,
) {
    /**
     * Clauses de garde séquentielles — extras invalides, session absente, inconnue, snapshot
     * illisible, révision en avance — avant le corps principal. Même motif que
     * [DefaultSessionCoordinator.dispatchLocked] : imbriquer ces refus produirait cinq niveaux
     * d'indentation pour une suite de conditions indépendantes.
     */
    @Suppress("ReturnCount")
    suspend fun handle(extras: ServiceCommandExtras): AlarmTriggerOutcome {
        val command = ServiceCommand.from(extras)
        if (command !is ServiceCommand.Valid) {
            technicalEventLog.log(TechnicalEventType.ALARM_RECEIVED, sessionId = null)
            return AlarmTriggerOutcome.InvalidCommand
        }
        // §17 : journalisé avant toute décision, y compris quand la suite refuse de déclencher.
        technicalEventLog.log(TechnicalEventType.ALARM_RECEIVED, sessionId = command.sessionId)

        val loaded = gateway.load()
        refusalFor(loaded, command)?.let { return it }

        readinessMonitor.evaluate((loaded as LoadResult.Present).snapshot, coordinator::dispatch)

        val freshSnapshot =
            when (val reloaded = gateway.load()) {
                is LoadResult.Present -> reloaded.snapshot
                is LoadResult.Absent -> return AlarmTriggerOutcome.NoSession
                is LoadResult.Unreadable -> return AlarmTriggerOutcome.UnreadableSnapshot
            }

        return AlarmTriggerOutcome.Dispatched(coordinator.dispatch(eventFactory.alarmFired(freshSnapshot)))
    }

    /**
     * `null` quand rien ne s'oppose au déclenchement. L'état source n'est **pas** contrôlé ici :
     * `TriggerReducer.onAlarmFired` exige déjà `ARMED` et refuse le reste (SPEC_CORE_KMP §5.1),
     * et le dupliquer côté Android contredirait « ne pas dupliquer une règle commune ».
     */
    private fun refusalFor(
        loaded: LoadResult,
        command: ServiceCommand.Valid,
    ): AlarmTriggerOutcome? =
        when (loaded) {
            is LoadResult.Unreadable -> {
                AlarmTriggerOutcome.UnreadableSnapshot
            }

            is LoadResult.Absent -> {
                AlarmTriggerOutcome.NoSession
            }

            is LoadResult.Present -> {
                when {
                    loaded.snapshot.sessionId != command.sessionId -> AlarmTriggerOutcome.UnknownSession
                    command.revision > loaded.snapshot.revision -> AlarmTriggerOutcome.RevisionAhead
                    else -> null
                }
            }
        }
}
