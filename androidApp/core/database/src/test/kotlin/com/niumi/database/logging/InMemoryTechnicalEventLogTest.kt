package com.niumi.database.logging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * SPEC_ANDROID §17 : 200 événements maximum, la plus ancienne supprimée au-delà ; le nom de
 * package n'est accepté que pour `BLOCK_APPLIED` (filtre porté par [TechnicalEventDetails]).
 */
class InMemoryTechnicalEventLogTest {
    private val log = InMemoryTechnicalEventLog(nowEpochMillis = { 42L })

    @Test
    fun keeps200MostRecentEntriesAndDropsTheOldest() =
        runTest {
            repeat(201) { index ->
                log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "session-$index")
            }

            val entries = log.recent()

            assertThat(entries).hasSize(200)
            assertThat(entries.first().sessionId).isEqualTo("session-1")
            assertThat(entries.last().sessionId).isEqualTo("session-200")
        }

    @Test
    fun packageNameIsKeptForBlockApplied() =
        runTest {
            log.log(
                TechnicalEventType.BLOCK_APPLIED,
                sessionId = "s1",
                detailsJson = TechnicalEventDetails.packageName("com.example.app"),
            )

            assertThat(log.recent().single().detailsJson)
                .isEqualTo(TechnicalEventDetails.packageName("com.example.app"))
        }

    @Test
    fun packageNameIsDroppedForAnyOtherType() =
        runTest {
            log.log(
                TechnicalEventType.ALARM_RECEIVED,
                sessionId = "s1",
                detailsJson = TechnicalEventDetails.packageName("com.example.app"),
            )

            assertThat(log.recent().single().detailsJson).isNull()
        }

    @Test
    fun entryCarriesTheInjectedTimestamp() =
        runTest {
            log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s1")

            assertThat(log.recent().single().occurredAtEpochMillis).isEqualTo(42L)
        }
}
