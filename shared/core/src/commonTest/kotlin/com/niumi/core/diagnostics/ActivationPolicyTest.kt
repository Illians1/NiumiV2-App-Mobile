package com.niumi.core.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NOW_EPOCH_MILLIS = 1_800_000_000_000L
private const val FUTURE_TRIGGER_EPOCH_MILLIS = NOW_EPOCH_MILLIS + 1_000L
private const val DEFAULT_APP_SELECTION_COUNT = 5

private fun input(
    checks: List<ReadinessCheckInput> = emptyList(),
    appSelectionCount: Int = DEFAULT_APP_SELECTION_COUNT,
    triggerAtEpochMillis: Long = FUTURE_TRIGGER_EPOCH_MILLIS,
    nowEpochMillis: Long = NOW_EPOCH_MILLIS,
    hasPairedBox: Boolean = true,
) = ActivationPolicyInput(checks, appSelectionCount, triggerAtEpochMillis, nowEpochMillis, hasPairedBox)

private fun check(
    id: String,
    severity: ReadinessSeverity,
    passed: Boolean,
) = ReadinessCheckInput(id, severity, passed)

class ActivationPolicyTest {
    @Test
    fun everythingGreenIsAllowedWithoutAnyReason() {
        val result = ActivationPolicy.evaluate(input())

        assertTrue(result.allowed)
        assertTrue(result.blockingReasons.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun failedBlockingForAlarmCheckRefusesWithItsCheckId() {
        val failedCheck = check("exact_alarm", ReadinessSeverity.BLOCKING_FOR_ALARM, passed = false)

        val result = ActivationPolicy.evaluate(input(checks = listOf(failedCheck)))

        assertFalseAllowed(result)
        assertEquals(
            listOf(ActivationReason(ActivationReasonCode.READINESS_BLOCKING_FOR_ALARM, "exact_alarm")),
            result.blockingReasons,
        )
    }

    @Test
    fun failedBlockingForNiumiExperienceCheckRefuses() {
        val failedCheck = check("nfc_enabled", ReadinessSeverity.BLOCKING_FOR_NIUMI_EXPERIENCE, passed = false)

        val result = ActivationPolicy.evaluate(input(checks = listOf(failedCheck)))

        assertFalseAllowed(result)
        assertEquals(
            listOf(ActivationReason(ActivationReasonCode.READINESS_BLOCKING_FOR_NIUMI_EXPERIENCE, "nfc_enabled")),
            result.blockingReasons,
        )
    }

    @Test
    fun failedWarningCheckAloneStaysAllowed() {
        val failedCheck = check("battery_optimized", ReadinessSeverity.WARNING, passed = false)

        val result = ActivationPolicy.evaluate(input(checks = listOf(failedCheck)))

        assertTrue(result.allowed)
        assertTrue(result.blockingReasons.isEmpty())
        assertEquals(
            listOf(ActivationReason(ActivationReasonCode.READINESS_WARNING, "battery_optimized")),
            result.warnings,
        )
    }

    @Test
    fun appSelectionCountZeroIsRefused() {
        val result = ActivationPolicy.evaluate(input(appSelectionCount = 0))

        assertFalseAllowed(result)
        assertEquals(
            listOf(ActivationReason(ActivationReasonCode.INVALID_APP_SELECTION, null)),
            result.blockingReasons,
        )
    }

    @Test
    fun appSelectionCountFiftyOneIsRefused() {
        val result = ActivationPolicy.evaluate(input(appSelectionCount = 51))

        assertFalseAllowed(result)
    }

    @Test
    fun appSelectionCountOneIsAccepted() {
        val result = ActivationPolicy.evaluate(input(appSelectionCount = 1))

        assertTrue(result.allowed)
    }

    @Test
    fun appSelectionCountFiftyIsAccepted() {
        val result = ActivationPolicy.evaluate(input(appSelectionCount = 50))

        assertTrue(result.allowed)
    }

    @Test
    fun triggerNotStrictlyFutureIsRefused() {
        val result = ActivationPolicy.evaluate(input(triggerAtEpochMillis = NOW_EPOCH_MILLIS))

        assertFalseAllowed(result)
        assertEquals(
            listOf(ActivationReason(ActivationReasonCode.TRIGGER_NOT_IN_FUTURE, null)),
            result.blockingReasons,
        )
    }

    @Test
    fun noPairedBoxIsRefused() {
        val result = ActivationPolicy.evaluate(input(hasPairedBox = false))

        assertFalseAllowed(result)
        assertEquals(
            listOf(ActivationReason(ActivationReasonCode.NO_PAIRED_BOX, null)),
            result.blockingReasons,
        )
    }

    @Test
    fun multipleSimultaneousRefusalsAreAllReported() {
        val failedCheck = check("exact_alarm", ReadinessSeverity.BLOCKING_FOR_ALARM, passed = false)

        val result =
            ActivationPolicy.evaluate(
                input(
                    checks = listOf(failedCheck),
                    appSelectionCount = 0,
                    triggerAtEpochMillis = NOW_EPOCH_MILLIS,
                    hasPairedBox = false,
                ),
            )

        assertFalseAllowed(result)
        assertEquals(4, result.blockingReasons.size)
    }

    private fun assertFalseAllowed(result: ActivationPolicyResult) {
        assertEquals(false, result.allowed)
        assertTrue(result.blockingReasons.isNotEmpty())
    }
}
