package com.niumi.core.schedule

import kotlin.test.Test
import kotlin.test.assertEquals

private const val TRIGGER_AT_EPOCH_MILLIS = 1_800_000_000_000L
private const val FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1_000L

class TriggerDelayPolicyTest {
    @Test
    fun beforeTriggerIsNotReached() {
        val outcome =
            TriggerDelayPolicy.evaluate(
                triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
                nowEpochMillis = TRIGGER_AT_EPOCH_MILLIS - 1,
            )

        assertEquals(TriggerDelayOutcome.NOT_REACHED, outcome)
    }

    @Test
    fun exactlyAtTriggerFiresNow() {
        val outcome =
            TriggerDelayPolicy.evaluate(
                triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
                nowEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
            )

        assertEquals(TriggerDelayOutcome.FIRE_NOW, outcome)
    }

    @Test
    fun exactlyFifteenMinutesLateStillFiresNow() {
        val outcome =
            TriggerDelayPolicy.evaluate(
                triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
                nowEpochMillis = TRIGGER_AT_EPOCH_MILLIS + FIFTEEN_MINUTES_MILLIS,
            )

        assertEquals(TriggerDelayOutcome.FIRE_NOW, outcome)
    }

    @Test
    fun oneMillisecondBeyondFifteenMinutesIsMissed() {
        val outcome =
            TriggerDelayPolicy.evaluate(
                triggerAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
                nowEpochMillis = TRIGGER_AT_EPOCH_MILLIS + FIFTEEN_MINUTES_MILLIS + 1,
            )

        assertEquals(TriggerDelayOutcome.MISSED, outcome)
    }
}
