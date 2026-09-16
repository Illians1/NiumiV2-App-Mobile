package com.niumi.core.domain

/**
 * Famille `ALARM_FIRED`, `ALARM_SOUND_STOPPED`, `TRIGGER_ELAPSED` (SPEC_CORE_KMP §5.1, §6, §11).
 * Extrait de `SessionEngine.reduce`, voir `ETAPE-07.md`, point 5. Le calcul du retard de 15 minutes
 * et l'incident `MISSED_TRIGGER_WINDOW` restent une décision du coordinateur natif (politique
 * horaire, étape 8) : ce réducteur se contente d'accepter l'incident déjà joint à l'événement.
 *
 * Les trois fonctions appliquent le repli du moteur via [applyPendingBlocking] : aucune sonnerie ni
 * demande de scan ne part sans que le blocage ait été demandé (§5.1, invariant §4).
 */
internal object TriggerReducer {
    internal fun onAlarmFired(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state != SessionState.ARMED) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val ringingSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                state = SessionState.RINGING,
                ringingAtEpochMillis = event.occurredAtEpochMillis,
            )
        val builder =
            SessionEffectBuilder(ringingSnapshot.sessionId, ringingSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
        val newSnapshot = applyPendingBlocking(ringingSnapshot, event, builder)
        builder.add(SessionEffectKind.START_RINGING)
        return SessionDecision(newSnapshot, builder.build(), emptyList())
    }

    internal fun onAlarmSoundStopped(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        val targetState =
            when (snapshot?.state) {
                SessionState.ARMED, SessionState.RINGING -> SessionState.AWAITING_NFC
                SessionState.TRIGGERED_AWAITING_NFC -> SessionState.TRIGGERED_AWAITING_NFC
                else -> null
            }
        if (snapshot == null || targetState == null) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val stoppedSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                state = targetState,
                alarmSoundStoppedAtEpochMillis = event.occurredAtEpochMillis,
            )
        val builder =
            SessionEffectBuilder(stoppedSnapshot.sessionId, stoppedSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
        val newSnapshot = applyPendingBlocking(stoppedSnapshot, event, builder)
        builder.add(SessionEffectKind.PRESENT_SCAN_REQUEST)
        return SessionDecision(newSnapshot, builder.build(), emptyList())
    }

    @Suppress("ReturnCount")
    internal fun onTriggerElapsed(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state != SessionState.ARMED) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        if (event.occurredAtEpochMillis < snapshot.wakeSchedule.triggerAtEpochMillis) {
            return reject(
                snapshot,
                listOf(
                    DomainViolation(
                        ViolationCode.TRIGGER_NOT_REACHED,
                        "L'heure contractuelle n'est pas encore atteinte.",
                    ),
                ),
            )
        }
        val incident = event.incident
        val elapsedSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                state = SessionState.TRIGGERED_AWAITING_NFC,
                triggerElapsedAtEpochMillis = event.occurredAtEpochMillis,
                health = healthAfter(snapshot.health, incident),
            )
        val builder =
            SessionEffectBuilder(elapsedSnapshot.sessionId, elapsedSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
        val newSnapshot = applyPendingBlocking(elapsedSnapshot, event, builder)
        builder.add(SessionEffectKind.PRESENT_SCAN_REQUEST)
        if (incident != null) {
            builder.add(SessionEffectKind.RECORD_INCIDENT, IncidentEffectPayload(incident))
        }
        return SessionDecision(newSnapshot, builder.build(), emptyList())
    }
}
