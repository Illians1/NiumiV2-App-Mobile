package com.niumi.core.domain

/**
 * Famille `VALID_NFC_SCANNED`, `INVALID_NFC_SCANNED` (SPEC_CORE_KMP §5.1, §6, §9, §12). Extrait de
 * `SessionEngine.reduce`, voir `ETAPE-07.md`, point 5.
 */
internal object NfcReducer {
    private val VALID_SCAN_SOURCE_STATES =
        setOf(SessionState.ARMED, SessionState.RINGING, SessionState.AWAITING_NFC, SessionState.TRIGGERED_AWAITING_NFC)

    internal fun onInvalidScan(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        // §4 : un NFC invalide ne modifie ni l'état, ni le blocage, ni le son — seule la revision
        // avance et le snapshot est republié.
        if (snapshot == null || snapshot.state in FINAL_STATES) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val newSnapshot = snapshot.copy(revision = snapshot.revision + 1)
        val effects =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .build()
        return SessionDecision(newSnapshot, effects, emptyList())
    }

    // Validateur à clauses de garde, même motif que `BoxPayloadParser.parse` : le premier échec
    // détermine le résultat de la libération (voir `ETAPE-02.md`).
    @Suppress("ReturnCount")
    internal fun onValidScan(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state !in VALID_SCAN_SOURCE_STATES) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
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
        val proof = requireNotNull(event.nfcProof) // garanti par SessionEventValidation
        if (!proof.matchesEvent(event)) {
            return reject(
                snapshot,
                listOf(
                    DomainViolation(
                        ViolationCode.UNEXPECTED_EVENT_PAYLOAD,
                        "nfcProof ne correspond pas à l'événement.",
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
                nfcVerifiedAtEpochMillis = event.occurredAtEpochMillis,
                releasingAtEpochMillis = event.occurredAtEpochMillis,
            )
        val effects =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .add(SessionEffectKind.CANCEL_ALARM)
                .add(SessionEffectKind.STOP_RINGING)
                .add(SessionEffectKind.CLEAR_SCAN_REQUEST)
                .add(SessionEffectKind.REMOVE_BLOCKING)
                .build()
        return SessionDecision(newSnapshot, effects, emptyList())
    }
}
