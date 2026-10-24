package com.niumi.core.domain

/**
 * Famille `DISARM_REQUESTED` (contrat 1.5, SPEC_CORE_KMP §5.1, §5.2, §12) : avant le coucher, un
 * changement de planning libère sans scan une session `ARMED` dont le blocage est encore en
 * attente. La décision commune 8 tient : l'état final `DISARMED` n'est atteint qu'après une phase
 * `RELEASING` réussie, avec les réducteurs de libération existants.
 */
internal object DisarmReducer {
    // Trois refus en clauses de garde, même motif que `NfcReducer.onValidScan`.
    @Suppress("ReturnCount")
    internal fun onDisarmRequested(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state != SessionState.ARMED) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val startsAt = snapshot.blockingSchedule.startsAtEpochMillis
        if (!snapshot.isBlockingPending || startsAt == null) {
            return reject(
                snapshot,
                listOf(DomainViolation(ViolationCode.BLOCKING_ALREADY_APPLIED, "Le blocage est déjà demandé.")),
            )
        }
        // Décision du 2026-10-24 : du coucher au scan, tout est verrouillé, même si l'alarme de
        // début n'a pas encore produit BLOCKING_START_ELAPSED. `startsAt` étant strictement
        // antérieur au réveil (§7.5), ce refus couvre aussi toute demande à ou après le réveil.
        if (event.occurredAtEpochMillis >= startsAt) {
            return reject(
                snapshot,
                listOf(
                    DomainViolation(
                        ViolationCode.BLOCKING_START_ALREADY_ELAPSED,
                        "L'heure du coucher est déjà atteinte.",
                    ),
                ),
            )
        }
        val newSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                state = SessionState.RELEASING,
                releaseTarget = ReleaseTarget.DISARMED,
                releasingAtEpochMillis = event.occurredAtEpochMillis,
                releasedBy = ReleaseMeans.PLAN_CHANGE,
            )
        // Ni sonnerie, ni demande de scan, ni blocage à retirer : le blocage est encore en attente.
        // Seul `CANCEL_ALARM` est requis pour `RELEASE_SUCCEEDED` (§6).
        val effects =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .add(SessionEffectKind.CANCEL_ALARM)
                .add(SessionEffectKind.CANCEL_BLOCKING_START)
                .build()
        return SessionDecision(newSnapshot, effects, emptyList())
    }
}
