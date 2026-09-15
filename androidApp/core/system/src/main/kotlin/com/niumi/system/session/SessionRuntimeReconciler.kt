package com.niumi.system.session

import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.AlarmScheduler

/**
 * Compare l'état métier aux deux sous-systèmes Android que `SessionReadinessMonitor` ne surveille
 * pas (SPEC_ANDROID §7.1, §18 ; étape 20) : une alarme `ARMED` réellement programmée, et le NFC.
 * Volontairement distinct du moniteur plutôt qu'étendu : celui-ci tourne sur `ARMED` (et
 * l'accessibilité seule au-delà), quand `nfcReady` doit rester surveillé sur cinq états — un
 * périmètre que sa table ne représente pas — et qu'`alarmScheduled` n'a rien à voir avec la
 * permission `EXACT_ALARM` déjà couverte. Les deux classes dédoubleraient sinon le même incident.
 *
 * §18 : « tente les réparations idempotentes autorisées, puis consigne un incident si l'écart
 * persiste. » Aucune réparation n'existe côté NFC — Android ne permet plus à une application
 * d'activer l'adaptateur depuis Android 10 — seule l'alarme est reprogrammable.
 */
class SessionRuntimeReconciler(
    private val probe: SessionRuntimeStatusProbe,
    private val alarmScheduler: AlarmScheduler,
    private val eventFactory: SessionEventFactory,
    private val incidentsReader: SessionIncidentsReader,
    private val technicalEventLog: TechnicalEventLog,
) {
    suspend fun reconcile(
        snapshot: SessionSnapshotDto,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): SessionSnapshotDto {
        var gaps = actionableGaps(snapshot)
        if (RuntimeGap.ALARM_NOT_SCHEDULED in gaps) {
            repairAlarm(snapshot)
            gaps = actionableGaps(snapshot)
        }

        var current = snapshot
        if (RuntimeGap.ALARM_NOT_SCHEDULED in gaps) {
            current =
                reportOnce(
                    current,
                    IncidentCodes.ALARM_PERMISSION_REVOKED,
                    TechnicalEventType.EXACT_ALARM_LOST,
                    dispatch,
                )
        }
        if (RuntimeGap.NFC_DISABLED in gaps) {
            current = reportOnce(current, IncidentCodes.NFC_DISABLED, TechnicalEventType.NFC_DISABLED, dispatch)
        }
        return current
    }

    private fun repairAlarm(snapshot: SessionSnapshotDto) {
        alarmScheduler.schedule(snapshot.sessionId, snapshot.revision, snapshot.wakeSchedule.triggerAtEpochMillis)
        technicalEventLog.log(TechnicalEventType.ALARM_RESCHEDULED, snapshot.sessionId)
    }

    /**
     * [RuntimeStatusGaps.of], moins l'alarme quand la permission d'alarme exacte est elle-même
     * absente. `SessionReadinessMonitor` (§13.1, contrôle `EXACT_ALARM`) couvre déjà ce cas, et
     * `reconcileArmed` interrompt volontairement la passe avant toute reprogrammation dans ce
     * scénario précis : y tenter `schedule()` ici serait un second essai sans espoir, pour la même
     * cause. Ce que cette classe traite est l'écart où la permission existe mais où l'alarme a
     * disparu quand même — un surcouche OEM qui l'efface, par exemple.
     */
    private fun actionableGaps(snapshot: SessionSnapshotDto): Set<RuntimeGap> {
        val gaps = RuntimeStatusGaps.of(probe.probe(snapshot.sessionId), snapshot.state)
        return if (RuntimeGap.ALARM_NOT_SCHEDULED in gaps && !alarmScheduler.canScheduleExact()) {
            gaps - RuntimeGap.ALARM_NOT_SCHEDULED
        } else {
            gaps
        }
    }

    /**
     * Un par code et par session, comme `SessionReadinessMonitor.recordIncidentOnce` : une
     * réparation qui échoue à chaque passe ne doit pas accumuler les incidents identiques (défaut
     * mesuré et corrigé à l'étape 16 pour le changement d'horloge).
     */
    private suspend fun reportOnce(
        snapshot: SessionSnapshotDto,
        incidentCode: String,
        technicalEventType: TechnicalEventType,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): SessionSnapshotDto {
        if (incidentsReader.incidents(snapshot.sessionId).any { it.code == incidentCode }) return snapshot
        technicalEventLog.log(technicalEventType, snapshot.sessionId)
        val incident = eventFactory.buildIncident(incidentCode, IncidentSeverityDto.CRITICAL)
        val result = dispatch(eventFactory.incidentReported(snapshot, incident))
        return (result as? DispatchResult.Applied)?.snapshot ?: snapshot
    }
}
