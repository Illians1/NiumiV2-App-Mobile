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
        // §4 (contrat 1.5) : `releasedBy` est renseigné avant l'entrée dans RELEASING,
        // `nfcVerifiedAt` existe si et seulement si `releasedBy == NFC_SCAN`, et `DISARMED` n'est
        // atteint que par un changement de planning. Un snapshot RELEASING forgé qui viole l'un de
        // ces invariants est refusé ici plutôt que de produire un état final incohérent.
        val target = snapshot.releaseTarget
        if (target == null || !snapshot.hasConsistentReleaseMeans(target)) {
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

                ReleaseTarget.DISARMED -> {
                    snapshot.copy(
                        revision = snapshot.revision + 1,
                        state = SessionState.DISARMED,
                        disarmedAtEpochMillis = event.occurredAtEpochMillis,
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

    private fun SessionSnapshot.hasConsistentReleaseMeans(target: ReleaseTarget): Boolean {
        val means = releasedBy ?: return false
        val scanned = means == ReleaseMeans.NFC_SCAN
        val disarmed = target == ReleaseTarget.DISARMED
        return scanned == (nfcVerifiedAtEpochMillis != null) && disarmed == (means == ReleaseMeans.PLAN_CHANGE)
    }
}
