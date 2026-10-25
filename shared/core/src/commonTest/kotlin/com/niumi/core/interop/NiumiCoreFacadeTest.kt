package com.niumi.core.interop

import com.niumi.core.diagnostics.ActivationPolicy
import com.niumi.core.diagnostics.ActivationPolicyInput
import com.niumi.core.diagnostics.ReadinessCheckInput
import com.niumi.core.diagnostics.ReadinessSeverity
import com.niumi.core.domain.ReleaseMeans
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionEventKind
import com.niumi.core.domain.SessionSnapshotFixtures
import com.niumi.core.domain.SessionState
import com.niumi.core.emergency.TranscriptionStatus
import com.niumi.core.schedule.BlockingScheduleStatus
import com.niumi.core.schedule.NextOccurrenceStatus
import com.niumi.core.schedule.TriggerDelayOutcome
import com.niumi.core.schedule.WakeScheduleCalculator
import com.niumi.core.schedule.WakeScheduleInput
import com.niumi.core.schedule.WakeScheduleStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TRIGGER_AT_EPOCH_MILLIS = 1_800_000_000_000L
private const val FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1_000L

/**
 * Vérifie que [NiumiCoreFacade] délègue fidèlement à `SessionEngine` et aux deux politiques
 * communes, sans jamais lancer (SPEC_CORE_KMP §14). Les tests NFC de la façade restent dans
 * `NiumiCoreFacadeNfcTest`, inchangés depuis l'étape 2.
 */
class NiumiCoreFacadeTest {
    private val facade = NiumiCoreFacade()

    @Test
    fun reduceOnAnIncoherentEventNeverThrowsAndReturnsTypedViolations() {
        val incoherentEvent =
            SessionEventDto(
                eventId = "not-a-canonical-uuid",
                sessionId = "not-a-canonical-uuid",
                kind = SessionEventKind.ACTIVATION_REQUESTED,
                occurredAtEpochMillis = 1_000L,
                expectedRevision = null,
                activationRequest = null,
                failureCode = null,
                incident = null,
            )

        val decision = facade.reduce(snapshot = null, event = incoherentEvent)

        assertTrue(decision.violations.isNotEmpty())
        assertTrue(decision.effects.isEmpty())
        assertEquals(null, decision.snapshot)
    }

    @Test
    fun computeWakeScheduleDelegatesToWakeScheduleCalculator() {
        val input = WakeScheduleInputDto(localTimeIso = "07:00", zoneId = "Europe/Paris", nowEpochMillis = 1_000L)
        val expected =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(input.localTimeIso, input.zoneId, input.nowEpochMillis),
            )

        val result = facade.computeWakeSchedule(input)

        assertEquals(expected.status, result.status)
        assertEquals(expected.schedule?.triggerAtEpochMillis, result.schedule?.triggerAtEpochMillis)
    }

    @Test
    fun computeWakeScheduleRelaysInvalidTimeWithoutThrowing() {
        val input = WakeScheduleInputDto(localTimeIso = "25:00", zoneId = "Europe/Paris", nowEpochMillis = 1_000L)

        val result = facade.computeWakeSchedule(input)

        assertEquals(WakeScheduleStatus.INVALID_TIME, result.status)
    }

    @Test
    fun evaluateActivationDelegatesToActivationPolicy() {
        val checks = listOf(ReadinessCheckInputDto("nfc_enabled", ReadinessSeverityDto.WARNING, passed = false))
        val input =
            ActivationPolicyInputDto(
                checks,
                appSelectionCount = 5,
                TRIGGER_AT_EPOCH_MILLIS,
                0L,
                pairedBoxCount = 1,
                null,
            )
        val expected =
            ActivationPolicy.evaluate(
                ActivationPolicyInput(
                    checks = listOf(ReadinessCheckInput("nfc_enabled", ReadinessSeverity.WARNING, passed = false)),
                    appSelectionCount = 5,
                    triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
                    nowEpochMillis = 0L,
                    pairedBoxCount = 1,
                    blockingStartsAtEpochMillis = null,
                ),
            )

        val result = facade.evaluateActivation(input)

        assertEquals(expected.allowed, result.allowed)
        assertEquals(expected.warnings.size, result.warnings.size)
    }

    @Test
    fun computeBlockingScheduleReturnsAnImmediateScheduleForANullLocalTime() {
        val result = facade.computeBlockingSchedule(blockingInput(localTimeIso = null))

        assertEquals(BlockingScheduleStatus.VALID, result.status)
        assertEquals(BlockingScheduleDto(), result.schedule)
    }

    @Test
    fun computeBlockingScheduleResolvesADeferredStartBeforeTheWakeUp() {
        val result = facade.computeBlockingSchedule(blockingInput(localTimeIso = "22:30"))

        assertEquals(BlockingScheduleStatus.VALID, result.status)
        assertEquals("22:30", result.schedule?.localTimeIso)
        assertEquals(1_788_899_400_000L, result.schedule?.startsAtEpochMillis)
    }

    @Test
    fun computeBlockingScheduleRefusesAStartThatIsNotBeforeTheWakeUp() {
        val result = facade.computeBlockingSchedule(blockingInput(localTimeIso = "08:00"))

        assertEquals(BlockingScheduleStatus.NOT_BEFORE_TRIGGER, result.status)
        assertNull(result.schedule)
    }

    @Test
    fun computeBlockingScheduleRelaysInvalidTimeAndUnknownZoneWithoutThrowing() {
        assertEquals(
            BlockingScheduleStatus.INVALID_TIME,
            facade.computeBlockingSchedule(blockingInput(localTimeIso = "25:00")).status,
        )
        assertEquals(
            BlockingScheduleStatus.UNKNOWN_ZONE,
            facade.computeBlockingSchedule(blockingInput(localTimeIso = "22:30", zoneId = "Not/AZone")).status,
        )
    }

    // 2026-09-08T20:00:00+02:00 et le réveil de 07:00 qui en découle, repères de
    // `BlockingScheduleCalculatorTest`.
    private fun blockingInput(
        localTimeIso: String?,
        zoneId: String = "Europe/Paris",
    ) = BlockingScheduleInputDto(
        localTimeIso = localTimeIso,
        zoneId = zoneId,
        nowEpochMillis = 1_788_890_400_000L,
        triggerAtEpochMillis = 1_788_930_000_000L,
    )

    @Test
    fun computeNextOccurrenceArmsTheNextCheckedMorningWithItsBedtime() {
        // 2026-09-10 (jeudi) 20:00 Europe/Paris → vendredi 7:00, coucher jeudi 22:30.
        val result = facade.computeNextOccurrence(nextOccurrenceInput(nowEpochMillis = 1_789_063_200_000L))

        assertEquals(NextOccurrenceStatus.VALID, result.status)
        val occurrence = requireNotNull(result.occurrence)
        assertEquals(
            WakeScheduleDto("2026-09-11", "07:00", "Europe/Paris", 1_789_102_800_000L),
            occurrence.wakeSchedule,
        )
        assertEquals(BlockingScheduleDto("2026-09-10", "22:30", 1_789_072_200_000L), occurrence.blockingSchedule)
        assertEquals(false, occurrence.blockingStartsImmediately)
    }

    @Test
    fun computeNextOccurrenceRelaysRefusalsWithoutThrowing() {
        val now = 1_789_063_200_000L

        assertEquals(
            NextOccurrenceStatus.UNKNOWN_ZONE,
            facade.computeNextOccurrence(nextOccurrenceInput(nowEpochMillis = now, zoneId = "Not/AZone")).status,
        )
        assertNull(
            facade.computeNextOccurrence(nextOccurrenceInput(nowEpochMillis = now, zoneId = "Not/AZone")).occurrence,
        )
    }

    @Test
    fun emergencyExitTextsListsTheValidatedTextWithItsLength() {
        val texts = facade.emergencyExitTexts()

        assertEquals(listOf("v1-01"), texts.map { it.id })
        assertEquals(190, texts.single().charCount)
        assertEquals(texts.single().text.length, texts.single().charCount)
    }

    @Test
    fun aCompleteTranscriptionThroughTheFacadeReleasesTheMorningSession() {
        // Parcours complet à la frontière : recopie vérifiée avec contexte, puis réduction de
        // l'événement qui porte la preuve (D2 : le matin, COMPLETED).
        val ringing = SessionSnapshotFixtures.ringingSnapshot().toDto()
        val eventId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
        val occurredAt = TRIGGER_AT_EPOCH_MILLIS + 120_000L
        val context = NfcVerificationContextDto(ringing.sessionId, eventId, ringing.revision, occurredAt)
        val text = facade.emergencyExitTexts().single()

        val transcription =
            facade.verifyEmergencyTranscription(EmergencyTranscriptionInputDto(text.id, text.text), context)
        val decision =
            facade.reduce(
                ringing,
                SessionEventDto(
                    eventId = eventId,
                    sessionId = ringing.sessionId,
                    kind = SessionEventKind.EMERGENCY_EXIT_CONFIRMED,
                    occurredAtEpochMillis = occurredAt,
                    expectedRevision = ringing.revision,
                    activationRequest = null,
                    failureCode = null,
                    incident = null,
                    emergencyProof = assertNotNull(transcription.proof),
                ),
            )

        assertEquals(TranscriptionStatus.COMPLETE, transcription.status)
        assertTrue(decision.violations.isEmpty())
        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.RELEASING, snapshot.state)
        assertEquals(ReleaseTarget.COMPLETED, snapshot.releaseTarget)
        assertEquals(ReleaseMeans.EMERGENCY_EXIT, snapshot.releasedBy)
    }

    @Test
    fun verifyEmergencyTranscriptionRelaysRefusalsWithoutThrowing() {
        assertEquals(
            TranscriptionStatus.UNKNOWN_TEXT,
            facade.verifyEmergencyTranscription(EmergencyTranscriptionInputDto("v9-99", "texte"), null).status,
        )
        val mismatch = facade.verifyEmergencyTranscription(EmergencyTranscriptionInputDto("v1-01", "Javis"), null)
        assertEquals(TranscriptionStatus.MISMATCH, mismatch.status)
        assertEquals(0, mismatch.mismatchWordIndex)
        assertNull(mismatch.proof)
    }

    @Test
    fun computeTimeToGetUpDelegatesToTheCalculator() {
        val result =
            facade.computeTimeToGetUp(
                TimeToGetUpInputDto(
                    TRIGGER_AT_EPOCH_MILLIS,
                    TRIGGER_AT_EPOCH_MILLIS + 4 * 60_000L,
                    declaredMinutes = 15,
                ),
            )

        assertEquals(TimeToGetUpResultDto(elapsedMinutes = 4, savedMinutes = 11), result)
    }

    private fun nextOccurrenceInput(
        nowEpochMillis: Long,
        days: Set<Int> = setOf(1, 2, 3, 4, 5),
        zoneId: String = "Europe/Paris",
    ) = NextOccurrenceInputDto(WakePlanDto(days, "07:00", "22:30"), zoneId, nowEpochMillis)

    @Test
    fun evaluateTriggerDelayNotReached() {
        val result =
            facade.evaluateTriggerDelay(TriggerDelayInputDto(TRIGGER_AT_EPOCH_MILLIS, TRIGGER_AT_EPOCH_MILLIS - 1))

        assertEquals(TriggerDelayOutcome.NOT_REACHED, result.outcome)
    }

    @Test
    fun evaluateTriggerDelayFiresNow() {
        val result =
            facade.evaluateTriggerDelay(TriggerDelayInputDto(TRIGGER_AT_EPOCH_MILLIS, TRIGGER_AT_EPOCH_MILLIS))

        assertEquals(TriggerDelayOutcome.FIRE_NOW, result.outcome)
    }

    @Test
    fun evaluateTriggerDelayMissed() {
        val nowEpochMillis = TRIGGER_AT_EPOCH_MILLIS + FIFTEEN_MINUTES_MILLIS + 1
        val result = facade.evaluateTriggerDelay(TriggerDelayInputDto(TRIGGER_AT_EPOCH_MILLIS, nowEpochMillis))

        assertEquals(TriggerDelayOutcome.MISSED, result.outcome)
    }
}
