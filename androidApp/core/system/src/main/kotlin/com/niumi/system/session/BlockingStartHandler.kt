package com.niumi.system.session

import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.ringing.ServiceCommand
import com.niumi.system.ringing.ServiceCommandExtras

/**
 * Ce qu'a fait le déclenchement du début de blocage. Miroir exact d'[AlarmTriggerOutcome] : les cinq
 * valeurs hors [Dispatched] sont des refus, aucun événement n'a atteint le moteur et l'état de la
 * session est inchangé.
 */
sealed interface BlockingStartOutcome {
    data class Dispatched(
        val result: DispatchResult,
    ) : BlockingStartOutcome

    data object InvalidCommand : BlockingStartOutcome

    data object NoSession : BlockingStartOutcome

    data object UnknownSession : BlockingStartOutcome

    data object RevisionAhead : BlockingStartOutcome

    data object UnreadableSnapshot : BlockingStartOutcome
}

/**
 * Traduit le déclenchement de l'alarme de début en `BLOCKING_START_ELAPSED` (SPEC_ANDROID §12.4 ;
 * SPEC_CORE_KMP §5.1, §11.3). `BlockingStartReceiver` ne fait que lire ses extras et déléguer ici :
 * toute la décision est ainsi prouvable en JVM, sans `Intent` ni composant Android.
 *
 * **Les mêmes gardes qu'[AlarmTriggerHandler], et pas une de plus.** Session du snapshot actif,
 * snapshot illisible refusé, et garde de révision **monotone** : seule une révision *supérieure* à
 * celle du snapshot est refusée, car l'extra du `PendingIntent` est figé au moment de
 * `SCHEDULE_BLOCKING_START` et la révision avance sans le réécrire (`INCIDENT_REPORTED` l'incrémente
 * sans produire d'effet de programmation). Refuser une révision inférieure ferait manquer le début du
 * blocage précisément quand l'appareil est déjà dégradé.
 *
 * **Ni l'état ni `blockingAppliedAtEpochMillis` ne sont contrôlés ici.** `BlockingReducer.onStartElapsed`
 * exige déjà `ARMED`, un blocage en attente et l'instant atteint, et refuse le reste
 * (SPEC_CORE_KMP §5.2) ; le dupliquer côté Android contredirait « ne jamais dupliquer une règle
 * commune ». C'est ce qui absorbe un déclenchement orphelin après `CANCELLED`, `COMPLETED` ou
 * `FAILED`, ou après un `CANCEL_BLOCKING_START` qui aurait échoué : le moteur refuse, le refus est
 * journalisé, rien ne se produit.
 *
 * **Tout déclenchement est journalisé avant décision** (`BLOCKING_START_RECEIVED`, §12.4, §17), y
 * compris ceux que la suite refuse. `BLOCKING_STARTED` ne peut pas y servir : il désigne l'exécution
 * **réussie** d'`APPLY_BLOCKING`, et l'employer pour un refus ferait lire au journal qu'un blocage a
 * commencé alors qu'il n'a rien commencé. Le type a donc été ajouté à la liste fermée de §17,
 * décision validée avec l'utilisateur le 2026-09-16 — c'est ce qui rend visible une alarme orpheline
 * ayant survécu à la fin de sa session, `CANCEL_BLOCKING_START` étant best-effort.
 *
 * **Pas de surveillance §13.1 ici**, contrairement à [AlarmTriggerHandler] : celle-ci existe pour
 * créer l'incident `ANDROID_ALARM_MUTED_BY_DND` avant que l'état ne devienne `RINGING`. Le début du
 * blocage laisse la session `ARMED`, état où `SessionReadinessMonitor` continue de surveiller les six
 * contrôles à chaque réconciliation. Aucun relevé n'est donc perdu, et aucune relecture de snapshot
 * n'est nécessaire avant le dispatch.
 */
class BlockingStartHandler(
    private val gateway: SessionPersistenceGateway,
    private val coordinator: SessionCoordinator,
    private val eventFactory: SessionEventFactory,
    private val technicalEventLog: TechnicalEventLog,
) {
    /**
     * Clauses de garde séquentielles avant le corps principal — même motif qu'[AlarmTriggerHandler.handle] :
     * imbriquer ces refus produirait quatre niveaux d'indentation pour des conditions indépendantes.
     */
    @Suppress("ReturnCount")
    suspend fun handle(extras: ServiceCommandExtras): BlockingStartOutcome {
        val command = ServiceCommand.from(extras)
        if (command !is ServiceCommand.Valid) {
            technicalEventLog.log(TechnicalEventType.BLOCKING_START_RECEIVED, sessionId = null)
            return BlockingStartOutcome.InvalidCommand
        }
        // §17 : journalisé avant toute décision, y compris quand la suite refuse d'appliquer le
        // blocage. Même convention qu'`ALARM_RECEIVED` dans `AlarmTriggerHandler`.
        technicalEventLog.log(TechnicalEventType.BLOCKING_START_RECEIVED, sessionId = command.sessionId)

        val loaded = gateway.load()
        val snapshot =
            when (loaded) {
                is LoadResult.Unreadable -> return BlockingStartOutcome.UnreadableSnapshot
                is LoadResult.Absent -> return BlockingStartOutcome.NoSession
                is LoadResult.Present -> loaded.snapshot
            }
        if (snapshot.sessionId != command.sessionId) return BlockingStartOutcome.UnknownSession
        if (command.revision > snapshot.revision) return BlockingStartOutcome.RevisionAhead

        return BlockingStartOutcome.Dispatched(
            coordinator.dispatch(eventFactory.blockingStartElapsed(snapshot, incident = null)),
        )
    }
}
