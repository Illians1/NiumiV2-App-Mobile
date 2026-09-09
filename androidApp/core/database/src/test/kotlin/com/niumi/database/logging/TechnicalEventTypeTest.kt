package com.niumi.database.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * `TechnicalEventType` est la liste blanche exacte de SPEC_ANDROID §17 : aucun type hors de cette
 * liste n'est représentable, par construction (enum fermé). Un ajout ou un renommage non spécifié
 * fait échouer ce test — c'est le seul contrôle possible sans introduire une API `String` libre.
 */
class TechnicalEventTypeTest {
    @Test
    fun matchesExactlyTheAllowedListOfSpecAndroidSection17() {
        val expected =
            listOf(
                "SESSION_PREPARING",
                "SESSION_ARMED",
                "SESSION_RELEASING",
                "SESSION_CANCELLED",
                "ALARM_SCHEDULED",
                "ALARM_RESCHEDULED",
                "ALARM_RECEIVED",
                "RINGING_STARTED",
                "AUDIO_START_FAILED",
                "FULL_SCREEN_DENIED",
                "EXACT_ALARM_LOST",
                "MISSED_TRIGGER_WINDOW",
                "ALARM_MUTED_BY_DND",
                "SESSION_READINESS_DEGRADED",
                "SCAN_REQUEST_NOTIFIED",
                "SCAN_REQUEST_CLEARED",
                "NFC_DISABLED",
                "NFC_SCAN_INVALID",
                "NFC_SCAN_VALID",
                "BLOCK_APPLIED",
                "ACCESSIBILITY_DISABLED",
                "PROCESS_RECREATED",
                "OEM_RESTRICTION_SUSPECTED",
                "SESSION_COMPLETED",
                "SESSION_FAILED",
                "RELEASE_PARTIAL_FAILURE",
            )

        assertThat(TechnicalEventType.entries.map { it.name }).containsExactlyElementsIn(expected)
    }
}
