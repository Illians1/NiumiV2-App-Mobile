package com.niumi.core.domain

/**
 * Famille `BLOCKING_START_ELAPSED` (SPEC_CORE_KMP §5.1, §5.2, §6, §8.3). L'événement ne change pas
 * l'état : il renseigne `blockingAppliedAtEpochMillis` et demande le blocage. Comme pour
 * `TRIGGER_ELAPSED`, le calcul du retard de 15 minutes et l'incident `MISSED_BLOCKING_START_WINDOW`
 * restent une décision du coordinateur natif ; ce réducteur se contente d'accepter l'incident déjà
 * joint à l'événement.
 */
internal object BlockingReducer {
    @Suppress("ReturnCount")
    internal fun onStartElapsed(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state != SessionState.ARMED) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        // Un blocage immédiat, ou déjà demandé, n'attend plus rien : le coordinateur ne doit jamais
        // produire cet événement pour lui (§5.2).
        if (!snapshot.isBlockingPending) {
            return reject(
                snapshot,
                listOf(
                    DomainViolation(
                        ViolationCode.BLOCKING_ALREADY_APPLIED,
                        "Le blocage de cette session a déjà été demandé.",
                    ),
                ),
            )
        }
        val startsAt = requireNotNull(snapshot.blockingSchedule.startsAtEpochMillis) // garanti par isBlockingPending
        if (event.occurredAtEpochMillis < startsAt) {
            return reject(
                snapshot,
                listOf(
                    DomainViolation(
                        ViolationCode.BLOCKING_START_NOT_REACHED,
                        "L'instant de début du blocage n'est pas encore atteint.",
                    ),
                ),
            )
        }

        val incident = event.incident
        val newSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                blockingAppliedAtEpochMillis = event.occurredAtEpochMillis,
                health = healthAfter(snapshot.health, incident),
            )
        val builder =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .add(SessionEffectKind.APPLY_BLOCKING)
        if (incident != null) {
            builder.add(SessionEffectKind.RECORD_INCIDENT, IncidentEffectPayload(incident))
        }
        return SessionDecision(newSnapshot, builder.build(), emptyList())
    }
}
