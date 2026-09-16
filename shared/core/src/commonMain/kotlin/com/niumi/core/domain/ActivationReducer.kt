package com.niumi.core.domain

/**
 * Famille `ACTIVATION_REQUESTED`, `ACTIVATION_SUCCEEDED`, `ACTIVATION_FAILED` (SPEC_CORE_KMP §5.1,
 * §6, §10). Extrait de `SessionEngine.reduce` pour rester sous les seuils de complexité de detekt
 * (`ETAPE-07.md`, point 5).
 */
internal object ActivationReducer {
    internal fun onRequested(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        // Seul l'état « aucune session » accepte une activation (SPEC_CORE_KMP §4 : une seule
        // session non finale à la fois). Un snapshot existant, final ou non, est refusé : le
        // coordinateur natif efface toujours le pointeur actif avant une nouvelle activation.
        if (snapshot != null) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val request = requireNotNull(event.activationRequest) // garanti par SessionEventValidation
        // Un blocage immédiat, ou différé dont l'instant de début est déjà dépassé, est demandé dès
        // l'activation (SPEC_CORE_KMP §8.3, dernier alinéa) ; sinon le coordinateur programmera
        // l'alarme de début.
        val startsAtEpochMillis = request.blockingSchedule.startsAtEpochMillis
        val blocksImmediately = startsAtEpochMillis == null || startsAtEpochMillis <= event.occurredAtEpochMillis
        val newSnapshot =
            SessionSnapshot(
                schemaVersion = SessionSnapshot.SCHEMA_VERSION,
                revision = 1,
                sessionId = event.sessionId,
                wakeSchedule = request.wakeSchedule,
                blockingSchedule = request.blockingSchedule,
                state = SessionState.PREPARING,
                releaseTarget = null,
                health = SessionHealth.HEALTHY,
                createdAtEpochMillis = event.occurredAtEpochMillis,
                armedAtEpochMillis = null,
                blockingAppliedAtEpochMillis = if (blocksImmediately) event.occurredAtEpochMillis else null,
                ringingAtEpochMillis = null,
                alarmSoundStoppedAtEpochMillis = null,
                triggerElapsedAtEpochMillis = null,
                nfcVerifiedAtEpochMillis = null,
                releasingAtEpochMillis = null,
                completedAtEpochMillis = null,
                cancelledAtEpochMillis = null,
                failureCode = null,
            )
        val effects =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .add(SessionEffectKind.SCHEDULE_ALARM)
                .add(
                    if (blocksImmediately) {
                        SessionEffectKind.APPLY_BLOCKING
                    } else {
                        SessionEffectKind.SCHEDULE_BLOCKING_START
                    },
                ).build()
        return SessionDecision(newSnapshot, effects, emptyList())
    }

    internal fun onSucceeded(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state != SessionState.PREPARING) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val newSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                state = SessionState.ARMED,
                armedAtEpochMillis = event.occurredAtEpochMillis,
            )
        val effects =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .build()
        return SessionDecision(newSnapshot, effects, emptyList())
    }

    internal fun onFailed(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        // §4 : une session déjà armée ne passe jamais à FAILED à cause d'un incident technique —
        // seule PREPARING accepte ACTIVATION_FAILED.
        if (snapshot == null || snapshot.state != SessionState.PREPARING) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val newSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                state = SessionState.FAILED,
                failureCode = event.failureCode,
            )
        val builder =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.CANCEL_ALARM)
        // `CANCEL_BLOCKING_START` n'est produit que pour un blocage différé : une alarme de début
        // n'existe que si `SCHEDULE_BLOCKING_START` l'a demandée, et une session immédiate n'en a
        // jamais. Écart à la lettre de la table SPEC_CORE_KMP §6, documenté dans cette même section
        // et validé avec l'utilisateur le 2026-09-16 (voir `ETAPE-22.md`).
        if (!newSnapshot.blockingSchedule.isImmediate) {
            builder.add(SessionEffectKind.CANCEL_BLOCKING_START)
        }
        builder
            .add(SessionEffectKind.REMOVE_BLOCKING)
            .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
            .add(SessionEffectKind.CLEAR_ACTIVE_SESSION)
        return SessionDecision(newSnapshot, builder.build(), emptyList())
    }
}
