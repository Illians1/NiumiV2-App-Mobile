package com.niumi.core.domain

import com.niumi.core.emergency.EmergencyExitProof
import com.niumi.core.nfc.NfcVerificationProof

/** États finaux de la machine (SPEC_CORE_KMP §5), partagés entre les réducteurs par famille. */
internal val FINAL_STATES: Set<SessionState> =
    setOf(SessionState.COMPLETED, SessionState.CANCELLED, SessionState.DISARMED, SessionState.FAILED)

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
 * Repli du moteur (SPEC_CORE_KMP §5.1, invariant §4) : une session `ARMED` ne quitte jamais cet état
 * vers `RINGING`, `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` sans que le blocage ait été demandé. Si
 * le début du blocage n'a pas encore été traité, la transition le demande elle-même — le champ est
 * renseigné et `APPLY_BLOCKING` ajouté au [builder], à la position où il est appelé. Sinon le
 * snapshot est renvoyé inchangé et aucun effet n'est ajouté.
 *
 * C'est le seul point du moteur qui écrit `blockingAppliedAtEpochMillis` en dehors de
 * [ActivationReducer] et [BlockingReducer]. Ce filet ne dispense jamais le coordinateur natif de
 * produire `BLOCKING_START_ELAPSED` à l'heure.
 */
internal fun applyPendingBlocking(
    snapshot: SessionSnapshot,
    event: SessionEvent,
    builder: SessionEffectBuilder,
): SessionSnapshot =
    if (snapshot.isBlockingPending) {
        builder.add(SessionEffectKind.APPLY_BLOCKING)
        snapshot.copy(blockingAppliedAtEpochMillis = event.occurredAtEpochMillis)
    } else {
        snapshot
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

/** Même contrôle que [NfcVerificationProof.matchesEvent], pour la preuve de recopie (contrat 1.5). */
internal fun EmergencyExitProof.matchesEvent(event: SessionEvent): Boolean =
    sessionId == event.sessionId &&
        eventId == event.eventId &&
        expectedRevision == event.expectedRevision &&
        verifiedAtEpochMillis == event.occurredAtEpochMillis

/**
 * Effets d'une libération prouvée vers `RELEASING` (SPEC_CORE_KMP §6), communs au scan valide et à
 * la sortie de secours : publier, annuler l'alarme, annuler le début d'un blocage différé, arrêter
 * la sonnerie, effacer la demande de scan, retirer le blocage.
 *
 * `CANCEL_BLOCKING_START` n'est produit que pour un blocage différé : une alarme de début n'existe
 * que si `SCHEDULE_BLOCKING_START` l'a demandée, et une session immédiate n'en a jamais. Écart à la
 * lettre de la table SPEC_CORE_KMP §6, documenté dans cette même section et validé avec
 * l'utilisateur le 2026-09-16 (voir `ETAPE-22.md`).
 */
internal fun provenReleaseEffects(releasing: SessionSnapshot): List<SessionEffect> {
    val builder =
        SessionEffectBuilder(releasing.sessionId, releasing.revision)
            .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
            .add(SessionEffectKind.CANCEL_ALARM)
    if (!releasing.blockingSchedule.isImmediate) {
        builder.add(SessionEffectKind.CANCEL_BLOCKING_START)
    }
    return builder
        .add(SessionEffectKind.STOP_RINGING)
        .add(SessionEffectKind.CLEAR_SCAN_REQUEST)
        .add(SessionEffectKind.REMOVE_BLOCKING)
        .build()
}
