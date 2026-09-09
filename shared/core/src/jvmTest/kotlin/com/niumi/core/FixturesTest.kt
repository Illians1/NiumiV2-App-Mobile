package com.niumi.core

import com.niumi.core.domain.ActivationRequest
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.core.domain.DomainViolation
import com.niumi.core.domain.IncidentSeverity
import com.niumi.core.domain.Platform
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionEffect
import com.niumi.core.domain.SessionEngine
import com.niumi.core.domain.SessionEvent
import com.niumi.core.domain.SessionEventKind
import com.niumi.core.domain.SessionHealth
import com.niumi.core.domain.SessionIncident
import com.niumi.core.domain.SessionSnapshot
import com.niumi.core.domain.SessionState
import com.niumi.core.domain.WakeSchedule
import com.niumi.core.schedule.WakeScheduleCalculator
import com.niumi.core.schedule.WakeScheduleInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

// Horodatages et identifiants de référence, partagés par toutes les fixtures de transition
// (indépendants de `commonTest/domain/SessionFixtures.kt` : ce fichier de `jvmTest` ne construit
// ses snapshots et événements qu'à partir de constructeurs publics, pour rester indépendant de la
// visibilité `internal` entre source sets — voir ETAPE-08.md).
private const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
private const val TRIGGER_AT_EPOCH_MILLIS = 1_800_000_000_000L
private val REFERENCE_WAKE_SCHEDULE =
    WakeSchedule(
        localDateIso = "2026-09-08",
        localTimeIso = "07:00",
        zoneIdAtActivation = "Europe/Paris",
        triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
    )

private fun readFixtureResource(name: String): String =
    requireNotNull(FixturesTest::class.java.classLoader.getResourceAsStream("fixtures/$name")) {
        "fixtures/$name introuvable sur le classpath de test"
    }.bufferedReader().readText()

class FixturesTest {
    @Serializable
    private data class WakeScheduleFixture(
        val name: String,
        val localTimeIso: String,
        val zoneId: String,
        val nowEpochMillis: Long,
        val expectedStatus: String,
        val expectedLocalDateIso: String? = null,
        val expectedLocalTimeIso: String? = null,
        val expectedZoneIdAtActivation: String? = null,
        val expectedTriggerAtEpochMillis: Long? = null,
    )

    @Test
    fun everyWakeScheduleFixtureMatchesItsExpectedResult() {
        val json = readFixtureResource("wake_schedules.json")
        val fixtures = Json.decodeFromString<List<WakeScheduleFixture>>(json)

        for (fixture in fixtures) {
            val result =
                WakeScheduleCalculator.compute(
                    WakeScheduleInput(fixture.localTimeIso, fixture.zoneId, fixture.nowEpochMillis),
                )

            assertEquals(fixture.expectedStatus, result.status.name, "fixture en échec : ${fixture.name}")
            val schedule = result.schedule
            if (fixture.expectedLocalDateIso != null) {
                assertEquals(
                    fixture.expectedLocalDateIso,
                    schedule?.localDateIso,
                    "fixture en échec : ${fixture.name}",
                )
            }
            if (fixture.expectedLocalTimeIso != null) {
                assertEquals(
                    fixture.expectedLocalTimeIso,
                    schedule?.localTimeIso,
                    "fixture en échec : ${fixture.name}",
                )
            }
            if (fixture.expectedZoneIdAtActivation != null) {
                assertEquals(
                    fixture.expectedZoneIdAtActivation,
                    schedule?.zoneIdAtActivation,
                    "fixture en échec : ${fixture.name}",
                )
            }
            if (fixture.expectedTriggerAtEpochMillis != null) {
                assertEquals(
                    fixture.expectedTriggerAtEpochMillis,
                    schedule?.triggerAtEpochMillis,
                    "fixture en échec : ${fixture.name}",
                )
            }
        }
    }

    @Serializable
    private data class EventFixture(
        val kind: String,
        val sessionId: String,
        val eventId: String,
        val occurredAtEpochMillis: Long,
        val expectedRevision: Long? = null,
        val activationRequestAppCount: Int? = null,
        val failureCode: String? = null,
        val incidentCode: String? = null,
        val incidentSeverity: String? = null,
    )

    @Serializable
    private data class TransitionFixture(
        val name: String,
        val snapshot: String,
        val event: EventFixture,
        val expectedState: String? = null,
        val expectedEffects: List<String> = emptyList(),
        val expectedViolations: List<String> = emptyList(),
    )

    @Test
    fun everySessionTransitionFixtureMatchesTheEngineDecision() {
        val json = readFixtureResource("session_transitions.json")
        val fixtures = Json.decodeFromString<List<TransitionFixture>>(json)
        val engine = SessionEngine()

        for (fixture in fixtures) {
            val snapshot = resolveSnapshot(fixture.snapshot)
            val event = resolveEvent(fixture.event)

            val decision = engine.reduce(snapshot, event)

            assertEquals(
                fixture.expectedState,
                decision.snapshot?.state?.name,
                "fixture en échec : ${fixture.name}",
            )
            assertEquals(
                fixture.expectedEffects,
                decision.effects.map { effect: SessionEffect -> effect.kind.name },
                "fixture en échec : ${fixture.name}",
            )
            assertEquals(
                fixture.expectedViolations,
                decision.violations.map { violation: DomainViolation -> violation.code },
                "fixture en échec : ${fixture.name}",
            )
        }
    }

    // Un seul snapshot par nom : chaque test fixe le champ nécessaire à la ligne qu'il illustre
    // (armé, en sonnerie, en libération…) sans reproduire l'ensemble du parcours normal — au
    // contraire de `SessionSnapshotFixtures` (commonTest), volontairement non réutilisé ici.
    private fun resolveSnapshot(name: String): SessionSnapshot? =
        when (name) {
            "none" -> null
            "preparing" -> snapshot(SessionState.PREPARING, revision = 1)
            "armed" -> snapshot(SessionState.ARMED, revision = 2)
            "ringing" -> snapshot(SessionState.RINGING, revision = 3)
            "triggeredAwaitingNfc" -> snapshot(SessionState.TRIGGERED_AWAITING_NFC, revision = 3)
            "releasingCompleted" -> releasingSnapshot(ReleaseTarget.COMPLETED)
            "releasingCancelled" -> releasingSnapshot(ReleaseTarget.CANCELLED)
            "completed" -> snapshot(SessionState.COMPLETED, revision = 6)
            "cancelled" -> snapshot(SessionState.CANCELLED, revision = 4)
            "failed" -> snapshot(SessionState.FAILED, revision = 2, failureCode = "ANDROID_ALARM_SCHEDULE_FAILED")
            else -> error("snapshot fixture inconnu : $name")
        }

    private fun releasingSnapshot(releaseTarget: ReleaseTarget): SessionSnapshot =
        snapshot(SessionState.RELEASING, revision = 5, releaseTarget = releaseTarget)

    private fun snapshot(
        state: SessionState,
        revision: Long,
        releaseTarget: ReleaseTarget? = null,
        failureCode: String? = null,
    ): SessionSnapshot {
        // `RELEASING` exige `nfcVerifiedAtEpochMillis` (invariant vérifié par `ReleaseReducer`,
        // SPEC_CORE_KMP §4) : les deux champs sont donc liés ici plutôt qu'indépendants.
        val nfcVerifiedAtEpochMillis = if (state == SessionState.RELEASING) TRIGGER_AT_EPOCH_MILLIS + 2_000L else null
        return SessionSnapshot(
            schemaVersion = SessionSnapshot.SCHEMA_VERSION,
            revision = revision,
            sessionId = SESSION_ID,
            wakeSchedule = REFERENCE_WAKE_SCHEDULE,
            state = state,
            releaseTarget = releaseTarget,
            health = SessionHealth.HEALTHY,
            createdAtEpochMillis = 1_700_000_000_000L,
            armedAtEpochMillis = null,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = nfcVerifiedAtEpochMillis,
            releasingAtEpochMillis = nfcVerifiedAtEpochMillis,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = failureCode,
        )
    }

    // `VALID_NFC_SCANNED` est volontairement absent de ce fichier de fixtures : sa preuve
    // (`NfcVerificationProof`) n'a ni forme sérialisable ni constructeur public (SPEC_CORE_KMP
    // §14), donc rien de représentable en JSON ne pourrait circuler jusqu'au moteur sans fabriquer
    // artificiellement une preuve — déjà couvert intégralement par `SessionEngineNfcTest`
    // (commonTest, étape 7). Voir ETAPE-08.md.
    private fun resolveEvent(fixture: EventFixture): SessionEvent {
        val activationRequest =
            fixture.activationRequestAppCount?.let { count ->
                ActivationRequest(REFERENCE_WAKE_SCHEDULE, AppSelectionSummary(count))
            }
        val incident =
            fixture.incidentCode?.let { code ->
                SessionIncident(
                    code = code,
                    severity = IncidentSeverity.valueOf(requireNotNull(fixture.incidentSeverity)),
                    occurredAtEpochMillis = fixture.occurredAtEpochMillis,
                    platform = Platform.ANDROID,
                )
            }
        return SessionEvent(
            eventId = fixture.eventId,
            sessionId = fixture.sessionId,
            kind = SessionEventKind.valueOf(fixture.kind),
            occurredAtEpochMillis = fixture.occurredAtEpochMillis,
            expectedRevision = fixture.expectedRevision,
            activationRequest = activationRequest,
            nfcProof = null,
            failureCode = fixture.failureCode,
            incident = incident,
        )
    }
}
