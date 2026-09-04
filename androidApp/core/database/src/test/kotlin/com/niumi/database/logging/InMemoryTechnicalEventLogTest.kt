package com.niumi.database.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §17 : 200 événements maximum, la plus ancienne supprimée au-delà ; le nom de
 * package n'est accepté que pour `BLOCK_APPLIED`.
 */
class InMemoryTechnicalEventLogTest {
    private val log = InMemoryTechnicalEventLog(nowEpochMillis = { 42L })

    @Test
    fun keeps200MostRecentEntriesAndDropsTheOldest() {
        repeat(201) { index ->
            log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "session-$index")
        }

        val entries = log.recent()

        assertThat(entries).hasSize(200)
        assertThat(entries.first().sessionId).isEqualTo("session-1")
        assertThat(entries.last().sessionId).isEqualTo("session-200")
    }

    @Test
    fun packageNameIsKeptForBlockApplied() {
        log.log(TechnicalEventType.BLOCK_APPLIED, sessionId = "s1", packageName = "com.example.app")

        assertThat(log.recent().single().packageName).isEqualTo("com.example.app")
    }

    @Test
    fun packageNameIsDroppedForAnyOtherType() {
        log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s1", packageName = "com.example.app")

        assertThat(log.recent().single().packageName).isNull()
    }

    @Test
    fun entryCarriesTheInjectedTimestamp() {
        log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s1")

        assertThat(log.recent().single().occurredAtEpochMillis).isEqualTo(42L)
    }
}
