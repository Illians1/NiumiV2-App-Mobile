package com.niumi.core.interop

import com.niumi.core.diagnostics.ActivationPolicy
import com.niumi.core.diagnostics.ActivationPolicyInput
import com.niumi.core.diagnostics.ReadinessCheckInput
import com.niumi.core.diagnostics.ReadinessSeverity
import com.niumi.core.domain.SessionEventKind
import com.niumi.core.schedule.TriggerDelayOutcome
import com.niumi.core.schedule.WakeScheduleCalculator
import com.niumi.core.schedule.WakeScheduleInput
import com.niumi.core.schedule.WakeScheduleStatus
import kotlin.test.Test
import kotlin.test.assertEquals
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
        val input = ActivationPolicyInputDto(checks, appSelectionCount = 5, TRIGGER_AT_EPOCH_MILLIS, 0L, true)
        val expected =
            ActivationPolicy.evaluate(
                ActivationPolicyInput(
                    checks = listOf(ReadinessCheckInput("nfc_enabled", ReadinessSeverity.WARNING, passed = false)),
                    appSelectionCount = 5,
                    triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
                    nowEpochMillis = 0L,
                    hasPairedBox = true,
                ),
            )

        val result = facade.evaluateActivation(input)

        assertEquals(expected.allowed, result.allowed)
        assertEquals(expected.warnings.size, result.warnings.size)
    }

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
