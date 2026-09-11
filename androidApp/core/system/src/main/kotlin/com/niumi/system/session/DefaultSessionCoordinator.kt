package com.niumi.system.session

import com.niumi.core.domain.IncidentCodes
import com.niumi.core.domain.ViolationCode
import com.niumi.core.interop.DomainViolationDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.database.mapping.EventFingerprint
import com.niumi.database.mapping.SessionEffectMapper.toPendingEffects
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MISSING_EXTRAS_CODE = "MISSING_ANDROID_EXTRAS"

/**
 * Implémentation unique de [SessionCoordinator] (SPEC_CORE_KMP §6, §6.1, §10, §12 ; SPEC_ANDROID
 * §7.1, §9.2, §11.3). [dispatch] et [reconcile] partagent un seul [Mutex] — non réentrant : tout le
 * corps s'exécute dans [dispatchLocked]/`reconcileLocked`, jamais en rappelant [dispatch].
 */
class DefaultSessionCoordinator(
    private val reducer: SessionReducer,
    private val gateway: SessionPersistenceGateway,
    private val effectDispatcher: EffectDispatcher,
    private val reconciler: SessionReconciler,
    private val eventFactory: SessionEventFactory,
) : SessionCoordinator {
    private val mutex = Mutex()

    override suspend fun dispatch(
        event: SessionEventDto,
        extras: AndroidSessionExtras?,
    ): DispatchResult = mutex.withLock { dispatchLocked(event, extras) }

    override suspend fun reconcile(reason: ReconcileReason): ReconcileResult =
        mutex.withLock { reconciler.reconcile(reason) { locked -> dispatchLocked(locked, extras = null) } }

    /**
     * Non verrouillante : appelée uniquement sous [mutex] déjà pris, par [dispatch] ou par
     * [reconciler] (fonction `dispatch` non verrouillante reçue en paramètre). La récursion pour
     * les événements de suivi (`ACTIVATION_SUCCEEDED`/`FAILED`, `RELEASE_SUCCEEDED`/`FAILED`,
     * `INCIDENT_REPORTED`) est bornée : ces quatre kinds n'ont eux-mêmes aucun effet requis
     * ([PhaseCompletion.requiredKindsFor] vide), donc leur propre appel à [dispatchLocked]
     * s'arrête après l'exécution de leurs effets, sans nouveau suivi.
     *
     * Clauses de garde séquentielles (snapshot illisible, session inconnue, doublon, extras
     * manquants, décision refusée) avant le corps principal — même motif que
     * `NfcReducer.onValidScan` (:shared:core), voir `ETAPE-07.md`.
     */
    @Suppress("ReturnCount")
    private suspend fun dispatchLocked(
        event: SessionEventDto,
        extras: AndroidSessionExtras?,
    ): DispatchResult {
        val loaded = gateway.load()
        val present = loaded as? LoadResult.Present
        val precheckRejection = precheckLoadAndDuplicate(loaded, present, event)
        if (precheckRejection != null) return precheckRejection

        val effectiveExtras = extras ?: present?.extras
        if (event.kind == SessionEventKindDto.ACTIVATION_REQUESTED && effectiveExtras == null) {
            return DispatchResult.Rejected(
                listOf(DomainViolationDto(MISSING_EXTRAS_CODE, "extras Android requis pour ACTIVATION_REQUESTED")),
            )
        }

        val decision = reducer.reduce(present?.snapshot, event)
        if (decision.violations.isNotEmpty()) {
            return DispatchResult.Rejected(decision.violations)
        }

        val newSnapshot = requireNotNull(decision.snapshot) { "SessionDecisionDto acceptée sans snapshot" }
        val extrasForCommit = requireNotNull(effectiveExtras) { "extras Android manquants pour une décision acceptée" }
        val pendingEffects = decision.effects.toPendingEffects()
        commitDecision(event, newSnapshot, pendingEffects, extrasForCommit)

        val execution = effectDispatcher.execute(pendingEffects, newSnapshot, extrasForCommit)
        val currentSnapshot = dispatchCollectedIncidents(newSnapshot, execution.incidents)

        return completePhase(event, currentSnapshot, execution.outcomes)
    }

    /**
     * [Duplicate] déjà reconnu, ou rejet : snapshot illisible, événement pour une autre session,
     * `eventId` déjà reçu avec un autre payload. `null` si rien de tout cela ne s'applique.
     * Clauses de garde séquentielles, même motif que [dispatchLocked].
     */
    @Suppress("ReturnCount")
    private suspend fun precheckLoadAndDuplicate(
        loaded: LoadResult,
        present: LoadResult.Present?,
        event: SessionEventDto,
    ): DispatchResult? {
        if (loaded is LoadResult.Unreadable) {
            return DispatchResult.Rejected(listOf(DomainViolationDto(IncidentCodes.SNAPSHOT_CORRUPTED, loaded.reason)))
        }
        if (present != null &&
            event.sessionId != present.snapshot.sessionId &&
            event.kind != SessionEventKindDto.ACTIVATION_REQUESTED
        ) {
            return DispatchResult.Rejected(
                listOf(DomainViolationDto(ViolationCode.UNKNOWN_SESSION, "eventId pour une autre session")),
            )
        }

        val existingReceipt = gateway.receipt(event.eventId)
        if (existingReceipt != null) {
            return if (existingReceipt.payloadSha256Hex == EventFingerprint.of(event)) {
                DispatchResult.Duplicate(existingReceipt)
            } else {
                DispatchResult.Rejected(
                    listOf(
                        DomainViolationDto(ViolationCode.EVENT_ID_CONFLICT, "eventId déjà reçu avec un autre payload"),
                    ),
                )
            }
        }
        return null
    }

    private suspend fun commitDecision(
        event: SessionEventDto,
        snapshot: SessionSnapshotDto,
        effects: List<PendingEffect>,
        extras: AndroidSessionExtras,
    ) {
        gateway.commit(
            StoredDecision(
                snapshot = snapshot,
                receipt =
                    EventReceipt(
                        eventId = event.eventId,
                        sessionId = snapshot.sessionId,
                        payloadSha256Hex = EventFingerprint.of(event),
                        appliedRevision = snapshot.revision,
                        receivedAtEpochMillis = event.occurredAtEpochMillis,
                    ),
                effects = effects,
                androidExtras = extras,
            ),
        )
    }

    /** Dispatche `INCIDENT_REPORTED` pour chaque incident collecté pendant l'exécution des effets
     * (SPEC_ANDROID §11.3), sauf si l'état est déjà final. Renvoie le dernier snapshot connu. */
    private suspend fun dispatchCollectedIncidents(
        snapshot: SessionSnapshotDto,
        incidents: List<SessionIncidentDto>,
    ): SessionSnapshotDto {
        var current = snapshot
        for (incident in incidents) {
            if (current.state !in SESSION_FINAL_STATES) {
                val followUp = dispatchLocked(eventFactory.incidentReported(current, incident), null)
                if (followUp is DispatchResult.Applied && followUp.snapshot != null) {
                    current = followUp.snapshot
                }
            }
        }
        return current
    }

    /** Referme la phase par l'événement de suivi requis, ou renvoie [DispatchResult.Applied]
     * directement si l'événement n'a aucun effet requis (SPEC_CORE_KMP §6, dernier alinéa). */
    private suspend fun completePhase(
        event: SessionEventDto,
        snapshot: SessionSnapshotDto,
        outcomes: EffectOutcomes,
    ): DispatchResult {
        val requiredKinds = PhaseCompletion.requiredKindsFor(event.kind)
        if (requiredKinds.isEmpty()) {
            return DispatchResult.Applied(snapshot, requiredEffectsSucceeded = true)
        }

        val satisfied = PhaseCompletion.isSatisfied(event.kind, outcomes)
        val followUpEvent = buildFollowUpEvent(event.kind, snapshot, satisfied, outcomes)
        val followUpResult = dispatchLocked(followUpEvent, null)
        return when (followUpResult) {
            is DispatchResult.Applied -> {
                DispatchResult.Applied(
                    followUpResult.snapshot,
                    requiredEffectsSucceeded = satisfied,
                )
            }

            else -> {
                followUpResult
            }
        }
    }

    private fun buildFollowUpEvent(
        kind: SessionEventKindDto,
        snapshot: SessionSnapshotDto,
        satisfied: Boolean,
        outcomes: EffectOutcomes,
    ): SessionEventDto =
        when (kind) {
            SessionEventKindDto.ACTIVATION_REQUESTED -> {
                if (satisfied) {
                    eventFactory.activationSucceeded(snapshot)
                } else {
                    eventFactory.activationFailed(
                        snapshot,
                        PhaseCompletion.firstFailureCode(kind, outcomes) ?: "ANDROID_ACTIVATION_FAILED",
                    )
                }
            }

            SessionEventKindDto.VALID_NFC_SCANNED -> {
                if (satisfied) {
                    eventFactory.releaseSucceeded(snapshot)
                } else {
                    eventFactory.releaseFailedWithPartialFailure(snapshot)
                }
            }

            else -> {
                error("Kind inattendu avec des effets requis : $kind")
            }
        }
}
