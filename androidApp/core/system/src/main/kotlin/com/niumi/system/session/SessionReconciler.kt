package com.niumi.system.session

import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.TriggerDelayInputDto
import com.niumi.core.interop.TriggerDelayOutcomeDto
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.readiness.ReadinessCheckId

/**
 * Reprend un état incomplet et compare l'état métier aux sous-systèmes Android (SPEC_CORE_KMP §6.1
 * dernier alinéa, §13 ; SPEC_ANDROID §9.2, §9.3, §13.1, §18). Reçoit `dispatch`, la fonction non
 * verrouillante de [DefaultSessionCoordinator], en paramètre plutôt qu'en dépendance construite :
 * évite un cycle de DI (le coordinateur détient le réconciliateur).
 *
 * Ordre d'une passe : rejeu de l'outbox d'abord, nouvelle décision ensuite (une seule, l'état
 * courant ne peut appartenir qu'à un cas du tableau). La politique de retard est toujours lue via
 * [NiumiCoreFacade.evaluateTriggerDelay], jamais recalculée côté Android (SPEC_ANDROID §9.3).
 */
class SessionReconciler(
    private val gateway: SessionPersistenceGateway,
    private val effectDispatcher: EffectDispatcher,
    private val sources: ReconcilerSources,
    private val facade: NiumiCoreFacade,
    private val eventFactory: SessionEventFactory,
    private val technicalEventLog: TechnicalEventLog,
) {
    // Clauses de garde séquentielles (snapshot illisible, absent) avant le corps principal — même
    // motif que `NfcReducer.onValidScan` (:shared:core), voir `ETAPE-07.md`.
    @Suppress("ReturnCount")
    suspend fun reconcile(
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): ReconcileResult {
        val loaded = gateway.load()
        if (loaded is LoadResult.Unreadable) {
            return ReconcileResult(sessionId = null, actions = listOf(ReconcileAction.SnapshotCorrupted))
        }
        val present = loaded as? LoadResult.Present ?: return ReconcileResult(sessionId = null, actions = emptyList())

        val snapshot = present.snapshot
        val sessionId = snapshot.sessionId
        val actions = mutableListOf<ReconcileAction>()

        // Réamorce le flux observé par l'interface avant toute décision. `SessionSnapshotPublisher`
        // vit en mémoire et repart à `null` à chaque démarrage du processus ; une session saine ne
        // produit aucune décision, donc aucun `PUBLISH_PLATFORM_SNAPSHOT` ne la republierait, et
        // l'accueil afficherait « Aucune session » alors que l'alarme est programmée (défaut mesuré
        // sur appareil à l'étape 14). Republier une valeur identique est sans effet : `StateFlow`
        // n'émet que sur changement.
        sources.snapshotPublisher.publish(snapshot)

        val replayable = gateway.pendingEffects(sessionId)
        if (replayable.isNotEmpty()) {
            val execution = effectDispatcher.execute(replayable, snapshot, present.extras)
            actions += ReconcileAction.OutboxReplayed(replayable.size)
            for (incident in execution.incidents) {
                if (snapshot.state !in SESSION_FINAL_STATES) {
                    actions +=
                        ReconcileAction.DecisionApplied(dispatch(eventFactory.incidentReported(snapshot, incident)))
                }
            }
        }

        when (snapshot.state) {
            SessionStateDto.PREPARING -> {
                reconcilePreparing(snapshot, dispatch, actions)
            }

            SessionStateDto.ARMED -> {
                reconcileArmed(snapshot, reason, dispatch, actions)
            }

            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            SessionStateDto.RELEASING,
            -> {
                // Rejeu de l'outbox déjà effectué ci-dessus, aucune nouvelle décision.
            }

            SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED -> {
                gateway.clearActive(sessionId)
                actions += ReconcileAction.PointerCleared
            }
        }

        return ReconcileResult(sessionId, actions)
    }

    private suspend fun reconcilePreparing(
        snapshot: SessionSnapshotDto,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ) {
        val blockingActive =
            (sources.blockedPackagesProjection.current() as? BlockedPackagesState.Active)?.sessionId ==
                snapshot.sessionId
        val followUp =
            if (sources.alarmScheduler.isScheduled(snapshot.sessionId) && blockingActive) {
                eventFactory.activationSucceeded(snapshot)
            } else {
                eventFactory.activationFailed(snapshot, "ANDROID_ACTIVATION_INTERRUPTED")
            }
        actions += ReconcileAction.DecisionApplied(dispatch(followUp))
    }

    /**
     * La surveillance de §13.1 remplace les deux contrôles ad hoc de l'étape 11 : les six
     * contrôles bloquants sont désormais évalués d'un seul tenant, chacun avec son incident et
     * sa notification. Le comportement d'origine est conservé sur un point clé — une permission
     * perdue interrompt la passe avant toute reprogrammation d'alarme. La condition porte sur
     * `failing`, l'état courant, et non sur `newlyReported` : un contrôle cassé depuis la passe
     * précédente n'est plus signalé mais reste cassé.
     */
    private suspend fun reconcileArmed(
        snapshot: SessionSnapshotDto,
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ) {
        val monitored = sources.readinessMonitor.evaluate(snapshot, dispatch)
        for (degradation in monitored.newlyReported) {
            actions += ReconcileAction.IncidentDispatched(degradation.incidentCode, IncidentSeverityDto.CRITICAL)
            actions += ReconcileAction.DecisionApplied(degradation.dispatchResult)
        }
        if (monitored.failing.any { it in PERMISSION_CHECKS }) return
        reconcileTriggerDelay(snapshot, reason, dispatch, actions)
    }

    private suspend fun reconcileTriggerDelay(
        snapshot: SessionSnapshotDto,
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ) {
        val triggerAt = snapshot.wakeSchedule.triggerAtEpochMillis
        val now = eventFactory.nowEpochMillis()
        when (facade.evaluateTriggerDelay(TriggerDelayInputDto(triggerAt, now)).outcome) {
            TriggerDelayOutcomeDto.NOT_REACHED -> {
                if (!sources.alarmScheduler.isScheduled(snapshot.sessionId)) {
                    rescheduleAlarm(snapshot, triggerAt, actions)
                }
            }

            TriggerDelayOutcomeDto.FIRE_NOW -> {
                if (reason == ReconcileReason.BEFORE_SCAN) {
                    actions +=
                        ReconcileAction.DecisionApplied(
                            dispatch(eventFactory.triggerElapsed(snapshot, incident = null)),
                        )
                } else {
                    rescheduleAlarm(snapshot, now, actions)
                }
            }

            TriggerDelayOutcomeDto.MISSED -> {
                technicalEventLog.log(TechnicalEventType.MISSED_TRIGGER_WINDOW, snapshot.sessionId)
                val incident =
                    eventFactory.buildIncident(
                        IncidentCodes.MISSED_TRIGGER_WINDOW,
                        IncidentSeverityDto.DEGRADED,
                    )
                actions += ReconcileAction.DecisionApplied(dispatch(eventFactory.triggerElapsed(snapshot, incident)))
            }
        }
    }

    private fun rescheduleAlarm(
        snapshot: SessionSnapshotDto,
        triggerAtEpochMillis: Long,
        actions: MutableList<ReconcileAction>,
    ) {
        sources.alarmScheduler.schedule(snapshot.sessionId, snapshot.revision, triggerAtEpochMillis)
        technicalEventLog.log(TechnicalEventType.ALARM_RESCHEDULED, snapshot.sessionId)
        actions += ReconcileAction.AlarmRescheduled(triggerAtEpochMillis)
    }

    private companion object {
        /**
         * Les deux pertes de permission qui rendent toute suite de la passe absurde :
         * reprogrammer une alarme sans accès aux alarmes exactes, ou poursuivre un blocage sans
         * service d'accessibilité. Les quatre autres contrôles de §13.1 sont signalés sans
         * interrompre la réconciliation — le réveil reste programmé, seul son audibilité ou son
         * affichage est compromis.
         */
        val PERMISSION_CHECKS =
            setOf(ReadinessCheckId.EXACT_ALARM, ReadinessCheckId.ACCESSIBILITY_SERVICE)
    }
}
