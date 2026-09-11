package com.niumi.system.readiness

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.notification.SessionWarningNotifier
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.SessionEventFactory

/** Un contrôle bloquant devenu faux pendant `ARMED`, avec l'incident qu'il a produit. */
data class ReadinessDegradation(
    val checkId: ReadinessCheckId,
    val incidentCode: String,
    val dispatchResult: DispatchResult,
)

/**
 * [failing] est l'état courant — tous les contrôles surveillés en échec, qu'ils aient déjà été
 * signalés ou non. [newlyReported] ne contient que ceux qui viennent de basculer. Les appelants
 * qui décident d'interrompre leur traitement (le réconciliateur, quand une permission est
 * perdue) doivent lire [failing] : un contrôle cassé depuis la passe précédente n'apparaît plus
 * dans [newlyReported] mais reste cassé.
 */
data class ReadinessMonitorResult(
    val failing: Set<ReadinessCheckId>,
    val newlyReported: List<ReadinessDegradation>,
)

/**
 * Surveillance de SPEC_ANDROID §13.1. Rejoue le diagnostic pendant une session `ARMED` et
 * produit **un incident et une notification par contrôle, une seule fois tant que ce contrôle ne
 * repasse pas vrai**.
 *
 * L'état de déduplication vit en mémoire, donc disparaît avec le processus : c'est assumé et
 * documenté en §13.1 (« l'avertissement est émis au plus tôt, jamais garanti immédiat »). Un
 * redémarrage du processus republie donc un avertissement encore valable, ce qui vaut mieux que
 * de le taire.
 *
 * Le service d'accessibilité n'est jamais sollicité comme sentinelle (§13.1, dernier alinéa) :
 * la surveillance n'a d'autre déclencheur que ses appelants.
 */
class SessionReadinessMonitor(
    private val readinessChecker: DeviceReadinessChecker,
    private val warningNotifier: SessionWarningNotifier,
    private val eventFactory: SessionEventFactory,
    private val technicalEventLog: TechnicalEventLog,
) {
    private val alreadyReported = mutableSetOf<ReadinessCheckId>()

    suspend fun evaluate(
        snapshot: SessionSnapshotDto,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): ReadinessMonitorResult {
        if (snapshot.state != SessionStateDto.ARMED) {
            // Hors `ARMED`, un avertissement encore affiché deviendrait mensonger.
            if (alreadyReported.isNotEmpty()) {
                alreadyReported.clear()
                warningNotifier.clearAll()
            }
            return ReadinessMonitorResult(failing = emptySet(), newlyReported = emptyList())
        }

        val report = readinessChecker.check(ReadinessInput(snapshot.wakeSchedule.triggerAtEpochMillis))
        val failing = mutableSetOf<ReadinessCheckId>()
        val newlyReported = mutableListOf<ReadinessDegradation>()

        MonitoredReadinessChecks.incidentCodes.forEach { (checkId, incidentCode) ->
            if (report.check(checkId).outcome == ReadinessOutcome.FAILED) {
                failing += checkId
                if (alreadyReported.add(checkId)) {
                    newlyReported += report(snapshot, checkId, incidentCode, dispatch)
                }
            } else if (alreadyReported.remove(checkId)) {
                warningNotifier.clear(checkId)
            }
        }

        return ReadinessMonitorResult(failing, newlyReported)
    }

    private suspend fun report(
        snapshot: SessionSnapshotDto,
        checkId: ReadinessCheckId,
        incidentCode: String,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): ReadinessDegradation {
        technicalEventLog.log(TechnicalEventType.SESSION_READINESS_DEGRADED, snapshot.sessionId)
        SPECIFIC_TECHNICAL_EVENTS[checkId]?.let { technicalEventLog.log(it, snapshot.sessionId) }
        warningNotifier.present(checkId)
        val incident = eventFactory.buildIncident(incidentCode, IncidentSeverityDto.CRITICAL)
        return ReadinessDegradation(
            checkId = checkId,
            incidentCode = incidentCode,
            dispatchResult = dispatch(eventFactory.incidentReported(snapshot, incident)),
        )
    }

    private companion object {
        /**
         * Événements techniques dédiés de SPEC_ANDROID §17, en plus de
         * `SESSION_READINESS_DEGRADED` qui est journalisé pour toute dégradation (§13.1). Les
         * contrôles sans événement propre n'en produisent qu'un.
         */
        val SPECIFIC_TECHNICAL_EVENTS =
            mapOf(
                ReadinessCheckId.EXACT_ALARM to TechnicalEventType.EXACT_ALARM_LOST,
                ReadinessCheckId.ACCESSIBILITY_SERVICE to TechnicalEventType.ACCESSIBILITY_DISABLED,
                ReadinessCheckId.DND_TOTAL_SILENCE to TechnicalEventType.ALARM_MUTED_BY_DND,
                ReadinessCheckId.FULL_SCREEN_INTENT to TechnicalEventType.FULL_SCREEN_DENIED,
            )
    }
}
