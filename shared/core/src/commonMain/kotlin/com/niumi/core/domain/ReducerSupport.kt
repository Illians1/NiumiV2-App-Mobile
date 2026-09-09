package com.niumi.core.domain

import com.niumi.core.nfc.NfcVerificationProof

/** États finaux de la machine (SPEC_CORE_KMP §5), partagés entre les réducteurs par famille. */
internal val FINAL_STATES: Set<SessionState> =
    setOf(SessionState.COMPLETED, SessionState.CANCELLED, SessionState.FAILED)

/**
 * Une décision refusée renvoie le [snapshot] reçu inchangé et une liste d'effets vide
 * (SPEC_CORE_KMP §6) : point d'entrée unique utilisé par tous les réducteurs pour garantir cet
 * invariant.
 */
internal fun reject(
    snapshot: SessionSnapshot?,
    violations: List<DomainViolation>,
): SessionDecision = SessionDecision(snapshot, emptyList(), violations)

/** Violation générique de transition, utilisée par tous les réducteurs par famille. */
internal fun invalidStateTransition(event: SessionEvent): DomainViolation =
    DomainViolation(
        ViolationCode.INVALID_STATE_TRANSITION,
        "${event.kind} n'est pas autorisé depuis l'état source (SPEC_CORE_KMP §5.1).",
    )

/**
 * Applique un incident à la santé courante (SPEC_CORE_KMP §7.3) : `WARNING` ne modifie pas
 * [current] ; `DEGRADED` ou `CRITICAL` la fait passer à `DEGRADED`, sans retour automatique à
 * `HEALTHY` tant que la session est active.
 */
internal fun healthAfter(
    current: SessionHealth,
    incident: SessionIncident?,
): SessionHealth =
    if (incident != null && incident.severity != IncidentSeverity.WARNING) {
        SessionHealth.DEGRADED
    } else {
        current
    }

/**
 * Vérifie que la preuve provient bien de la transition en cours (SPEC_CORE_KMP §6, dernier
 * alinéa) : comparaison champ par champ, jamais par `==` sur la preuve elle-même — voir le KDoc de
 * [NfcVerificationProof] (`equals` structurel volontairement absent).
 */
internal fun NfcVerificationProof.matchesEvent(event: SessionEvent): Boolean =
    sessionId == event.sessionId &&
        eventId == event.eventId &&
        expectedRevision == event.expectedRevision &&
        verifiedAtEpochMillis == event.occurredAtEpochMillis
