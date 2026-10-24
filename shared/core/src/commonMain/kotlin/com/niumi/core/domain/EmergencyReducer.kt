package com.niumi.core.domain

/**
 * Famille `EMERGENCY_EXIT_CONFIRMED` (contrat 1.5, SPEC_CORE_KMP §5.1, §5.2, §12) : la recopie
 * complète d'un texte, prouvée par [com.niumi.core.emergency.EmergencyExitProof], libère la session
 * comme un scan valide — mêmes états sources, mêmes cibles, mêmes effets —, avec
 * `releasedBy = EMERGENCY_EXIT` et sans `nfcVerifiedAtEpochMillis`. Le matin, la session se termine
 * (`COMPLETED`, D2, décision du 2026-10-24) ; le soir, elle est annulée (`CANCELLED`).
 */
internal object EmergencyReducer {
    private val SOURCE_STATES =
        setOf(SessionState.ARMED, SessionState.RINGING, SessionState.AWAITING_NFC, SessionState.TRIGGERED_AWAITING_NFC)

    // Clauses de garde, même motif que `NfcReducer.onValidScan`.
    @Suppress("ReturnCount")
    internal fun onConfirmed(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        // Avant le coucher, le blocage est en attente : rien à fuir, le planning se désarme sans
        // scan (`DISARM_REQUESTED`), la sortie de secours n'a pas lieu d'être.
        if (snapshot == null || !snapshot.acceptsEmergencyExit()) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        // Symétrie avec le scan (§4) : le coordinateur produit TRIGGER_ELAPSED d'abord, sans quoi
        // une sortie du matin mènerait à CANCELLED.
        if (snapshot.state == SessionState.ARMED &&
            event.occurredAtEpochMillis >= snapshot.wakeSchedule.triggerAtEpochMillis
        ) {
            return reject(
                snapshot,
                listOf(
                    DomainViolation(ViolationCode.TRIGGER_ALREADY_ELAPSED, "L'heure contractuelle est déjà atteinte."),
                ),
            )
        }
        val proof = requireNotNull(event.emergencyProof) // garanti par SessionEventValidation
        if (!proof.matchesEvent(event)) {
            return reject(
                snapshot,
                listOf(
                    DomainViolation(
                        ViolationCode.UNEXPECTED_EVENT_PAYLOAD,
                        "emergencyProof ne correspond pas à l'événement.",
                    ),
                ),
            )
        }

        val releaseTarget =
            when (snapshot.state) {
                SessionState.ARMED -> ReleaseTarget.CANCELLED
                else -> ReleaseTarget.COMPLETED
            }
        val newSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                state = SessionState.RELEASING,
                releaseTarget = releaseTarget,
                releasingAtEpochMillis = event.occurredAtEpochMillis,
                releasedBy = ReleaseMeans.EMERGENCY_EXIT,
            )
        return SessionDecision(newSnapshot, provenReleaseEffects(newSnapshot), emptyList())
    }

    private fun SessionSnapshot.acceptsEmergencyExit(): Boolean =
        state in SOURCE_STATES && !(state == SessionState.ARMED && isBlockingPending)
}
