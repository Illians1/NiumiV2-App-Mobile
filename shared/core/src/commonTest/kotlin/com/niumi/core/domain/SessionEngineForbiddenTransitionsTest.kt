package com.niumi.core.domain

import com.niumi.core.domain.EmergencyExitEventFixtures.emergencyExitConfirmedEvent
import com.niumi.core.domain.NfcScanEventFixtures.validNfcScannedEvent
import com.niumi.core.domain.SessionActivationEventFixtures.activationFailedEvent
import com.niumi.core.domain.SessionActivationEventFixtures.activationRequestedEvent
import com.niumi.core.domain.SessionActivationEventFixtures.activationSucceededEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.alarmFiredEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.alarmSoundStoppedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.blockingStartElapsedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.disarmRequestedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.incidentReportedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.invalidNfcScannedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseFailedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseSucceededEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.triggerElapsedEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.awaitingNfcSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.cancelledSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.completedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.disarmedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.failedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.preparingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.releasingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.ringingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.triggeredAwaitingNfcSnapshot
import kotlin.test.Test
import kotlin.test.assertTrue

// États non finaux (SPEC_CORE_KMP §5) : source acceptée par INCIDENT_REPORTED et
// INVALID_NFC_SCANNED, dont §5.2 dit seulement « état actif » sans lister les états un par un.
// Interprétation retenue à l'implémentation des réducteurs : tout état non final. Voir
// `ETAPE-07.md`.
private val ACTIVE_STATES: Set<SessionState> =
    SessionState.entries.toSet() -
        setOf(SessionState.COMPLETED, SessionState.CANCELLED, SessionState.DISARMED, SessionState.FAILED)

// Couples (état source, événement) explicitement autorisés par la table de SPEC_CORE_KMP §5.1.
// `null` représente l'absence de snapshot (« aucun » dans la table).
private val ALLOWED_PAIRS: Set<Pair<SessionState?, SessionEventKind>> =
    buildSet {
        add(null to SessionEventKind.ACTIVATION_REQUESTED)
        add(SessionState.PREPARING to SessionEventKind.ACTIVATION_SUCCEEDED)
        add(SessionState.PREPARING to SessionEventKind.ACTIVATION_FAILED)
        add(SessionState.ARMED to SessionEventKind.ALARM_FIRED)
        add(SessionState.ARMED to SessionEventKind.ALARM_SOUND_STOPPED)
        add(SessionState.RINGING to SessionEventKind.ALARM_SOUND_STOPPED)
        add(SessionState.TRIGGERED_AWAITING_NFC to SessionEventKind.ALARM_SOUND_STOPPED)
        add(SessionState.ARMED to SessionEventKind.TRIGGER_ELAPSED)
        // §5.1 n'autorise ce couple que pour une session dont le blocage n'est pas encore demandé.
        // Le snapshot `ARMED` de cette table est à blocage immédiat : le refus correspondant
        // (`BLOCKING_ALREADY_APPLIED`) et l'acceptation d'une session en attente sont prouvés par
        // `SessionEngineBlockingStartTest`.
        add(SessionState.ARMED to SessionEventKind.BLOCKING_START_ELAPSED)
        add(SessionState.ARMED to SessionEventKind.VALID_NFC_SCANNED)
        // Contrat 1.5 : comme `BLOCKING_START_ELAPSED`, autorisé seulement blocage en attente et
        // avant le coucher. Le refus sur le snapshot `ARMED` immédiat de cette table et
        // l'acceptation d'une session en attente sont prouvés par `SessionEngineDisarmTest`.
        add(SessionState.ARMED to SessionEventKind.DISARM_REQUESTED)
        add(SessionState.RINGING to SessionEventKind.VALID_NFC_SCANNED)
        add(SessionState.AWAITING_NFC to SessionEventKind.VALID_NFC_SCANNED)
        add(SessionState.TRIGGERED_AWAITING_NFC to SessionEventKind.VALID_NFC_SCANNED)
        // Contrat 1.5 : la sortie de secours prouvée, comme le scan, sauf depuis `ARMED` en attente
        // (refus prouvé par `SessionEngineEmergencyExitTest` ; le snapshot `ARMED` de cette table
        // a son blocage appliqué).
        add(SessionState.ARMED to SessionEventKind.EMERGENCY_EXIT_CONFIRMED)
        add(SessionState.RINGING to SessionEventKind.EMERGENCY_EXIT_CONFIRMED)
        add(SessionState.AWAITING_NFC to SessionEventKind.EMERGENCY_EXIT_CONFIRMED)
        add(SessionState.TRIGGERED_AWAITING_NFC to SessionEventKind.EMERGENCY_EXIT_CONFIRMED)
        add(SessionState.RELEASING to SessionEventKind.RELEASE_FAILED)
        add(SessionState.RELEASING to SessionEventKind.RELEASE_SUCCEEDED)
        ACTIVE_STATES.forEach { add(it to SessionEventKind.INCIDENT_REPORTED) }
        ACTIVE_STATES.forEach { add(it to SessionEventKind.INVALID_NFC_SCANNED) }
    }

/**
 * Table exhaustive états × événements (SPEC_CORE_KMP §17) : chaque couple absent de
 * SPEC_CORE_KMP §5.1 doit produire au moins une violation et aucun effet, quel que soit le motif
 * exact de refus.
 */
class SessionEngineForbiddenTransitionsTest {
    private val engine = SessionEngine()
    private val revision = 10L

    @Test
    fun everyPairAbsentFromTheTransitionTableIsRejected() {
        val sourceStates: List<SessionState?> = listOf(null) + SessionState.entries.toList()

        for (state in sourceStates) {
            for (kind in SessionEventKind.entries) {
                if ((state to kind) in ALLOWED_PAIRS) continue

                val snapshot = snapshotOf(state)
                val event = eventOf(kind)
                val decision = engine.reduce(snapshot, event)

                assertTrue(decision.violations.isNotEmpty(), "attendu un refus pour ($state, $kind)")
                assertTrue(decision.effects.isEmpty(), "aucun effet attendu pour ($state, $kind)")
            }
        }
    }

    private fun snapshotOf(state: SessionState?): SessionSnapshot? =
        when (state) {
            null -> null
            SessionState.PREPARING -> preparingSnapshot(revision = revision)
            SessionState.ARMED -> armedSnapshot(revision = revision)
            SessionState.RINGING -> ringingSnapshot(revision = revision)
            SessionState.AWAITING_NFC -> awaitingNfcSnapshot(revision = revision)
            SessionState.TRIGGERED_AWAITING_NFC -> triggeredAwaitingNfcSnapshot(revision = revision)
            SessionState.RELEASING -> releasingSnapshot(ReleaseTarget.COMPLETED, revision = revision)
            SessionState.COMPLETED -> completedSnapshot(revision = revision)
            SessionState.CANCELLED -> cancelledSnapshot(revision = revision)
            SessionState.DISARMED -> disarmedSnapshot(revision = revision)
            SessionState.FAILED -> failedSnapshot(revision = revision)
        }

    private fun eventOf(kind: SessionEventKind): SessionEvent =
        when (kind) {
            SessionEventKind.ACTIVATION_REQUESTED -> {
                activationRequestedEvent()
            }

            SessionEventKind.ACTIVATION_SUCCEEDED -> {
                activationSucceededEvent(expectedRevision = revision)
            }

            SessionEventKind.ACTIVATION_FAILED -> {
                activationFailedEvent(expectedRevision = revision)
            }

            SessionEventKind.ALARM_FIRED -> {
                alarmFiredEvent(expectedRevision = revision)
            }

            SessionEventKind.ALARM_SOUND_STOPPED -> {
                alarmSoundStoppedEvent(expectedRevision = revision)
            }

            SessionEventKind.TRIGGER_ELAPSED -> {
                triggerElapsedEvent(expectedRevision = revision)
            }

            SessionEventKind.BLOCKING_START_ELAPSED -> {
                blockingStartElapsedEvent(expectedRevision = revision)
            }

            SessionEventKind.VALID_NFC_SCANNED -> {
                validNfcScannedEvent(
                    expectedRevision = revision,
                    occurredAtEpochMillis =
                        TRIGGER_AT_EPOCH_MILLIS - 1_000L,
                )
            }

            SessionEventKind.INVALID_NFC_SCANNED -> {
                invalidNfcScannedEvent(expectedRevision = revision)
            }

            SessionEventKind.RELEASE_SUCCEEDED -> {
                releaseSucceededEvent(expectedRevision = revision)
            }

            SessionEventKind.RELEASE_FAILED -> {
                releaseFailedEvent(expectedRevision = revision)
            }

            SessionEventKind.INCIDENT_REPORTED -> {
                incidentReportedEvent(expectedRevision = revision)
            }

            SessionEventKind.DISARM_REQUESTED -> {
                disarmRequestedEvent(expectedRevision = revision)
            }

            SessionEventKind.EMERGENCY_EXIT_CONFIRMED -> {
                emergencyExitConfirmedEvent(
                    expectedRevision = revision,
                    occurredAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS - 1_000L,
                )
            }
        }
}
