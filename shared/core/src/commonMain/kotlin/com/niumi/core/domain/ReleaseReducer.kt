package com.niumi.core.domain

/**
 * Famille `RELEASE_SUCCEEDED`, `RELEASE_FAILED` (SPEC_CORE_KMP §5.1, §6, §12). Extrait de
 * `SessionEngine.reduce`, voir `ETAPE-07.md`, point 5.
 */
internal object ReleaseReducer {
    internal fun onFailed(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state != SessionState.RELEASING) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val incident = requireNotNull(event.incident) // garanti par SessionEventValidation
        val newSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                health = healthAfter(snapshot.health, incident),
            )
        val effects =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.RECORD_INCIDENT, IncidentEffectPayload(incident))
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .build()
        return SessionDecision(newSnapshot, effects, emptyList())
    }

    @Suppress("ReturnCount")
    internal fun onSucceeded(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state != SessionState.RELEASING) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        // §4 : nfcVerifiedAt doit exister avant l'entrée dans RELEASING. Un snapshot RELEASING
        // forgé sans nfcVerifiedAt ni releaseTarget viole cet invariant et est refusé ici plutôt
        // que de produire un état final incohérent.
        val target = snapshot.releaseTarget
        if (target == null || snapshot.nfcVerifiedAtEpochMillis == null) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val newSnapshot =
            when (target) {
                ReleaseTarget.COMPLETED -> {
                    snapshot.copy(
                        revision = snapshot.revision + 1,
                        state = SessionState.COMPLETED,
                        completedAtEpochMillis = event.occurredAtEpochMillis,
                    )
                }

                ReleaseTarget.CANCELLED -> {
                    snapshot.copy(
                        revision = snapshot.revision + 1,
                        state = SessionState.CANCELLED,
                        cancelledAtEpochMillis = event.occurredAtEpochMillis,
                    )
                }
            }
        val effects =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .add(SessionEffectKind.CLEAR_ACTIVE_SESSION)
                .build()
        return SessionDecision(newSnapshot, effects, emptyList())
    }
}
