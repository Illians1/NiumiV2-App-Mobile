package com.niumi.database.mapping

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ActivationRequestDto
import com.niumi.core.interop.AppSelectionSummaryDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.WakeScheduleDto
import org.junit.Test

/**
 * Empreinte canonique d'un `SessionEventDto` (SPEC_CORE_KMP §6.1) : SHA-256 hexadécimal d'un JSON
 * dont les clés sont triées récursivement, `eventId` retiré de l'objet racine (`nfcProof` est déjà
 * `@Transient`). Sert à reconnaître un doublon d'événement sans nouvel appel à `reduce()`.
 */
class EventFingerprintTest {
    private val referenceEvent =
        SessionEventDto(
            eventId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            sessionId = "11111111-1111-1111-1111-111111111111",
            kind = SessionEventKindDto.ACTIVATION_REQUESTED,
            occurredAtEpochMillis = 1_700_000_000_000L,
            expectedRevision = null,
            activationRequest =
                ActivationRequestDto(
                    wakeSchedule =
                        WakeScheduleDto(
                            localDateIso = "2026-09-08",
                            localTimeIso = "07:00",
                            zoneIdAtActivation = "Europe/Paris",
                            triggerAtEpochMillis = 1_800_000_000_000L,
                        ),
                    appSelection = AppSelectionSummaryDto(count = 5),
                ),
            failureCode = null,
            incident = null,
        )

    @Test
    fun sameEventProducesTheSameFingerprintTwice() {
        assertThat(EventFingerprint.of(referenceEvent)).isEqualTo(EventFingerprint.of(referenceEvent))
    }

    @Test
    fun differentEventIdProducesTheSameFingerprint() {
        val other = referenceEvent.copy(eventId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")

        assertThat(EventFingerprint.of(other)).isEqualTo(EventFingerprint.of(referenceEvent))
    }

    @Test
    fun differentSessionIdProducesADifferentFingerprint() {
        val other = referenceEvent.copy(sessionId = "22222222-2222-2222-2222-222222222222")

        assertThat(EventFingerprint.of(other)).isNotEqualTo(EventFingerprint.of(referenceEvent))
    }

    @Test
    fun differentOccurredAtEpochMillisProducesADifferentFingerprint() {
        val other = referenceEvent.copy(occurredAtEpochMillis = referenceEvent.occurredAtEpochMillis + 1)

        assertThat(EventFingerprint.of(other)).isNotEqualTo(EventFingerprint.of(referenceEvent))
    }

    @Test
    fun differentActivationRequestProducesADifferentFingerprint() {
        val other =
            referenceEvent.copy(
                activationRequest =
                    requireNotNull(referenceEvent.activationRequest)
                        .copy(appSelection = AppSelectionSummaryDto(count = 6)),
            )

        assertThat(EventFingerprint.of(other)).isNotEqualTo(EventFingerprint.of(referenceEvent))
    }

    @Test
    fun canonicalJsonNeverContainsTheEventIdKey() {
        val json = EventFingerprint.canonicalJson(referenceEvent)

        assertThat(json).doesNotContain("\"eventId\"")
    }

    @Test
    fun canonicalJsonNeverContainsTheProofKey() {
        val json = EventFingerprint.canonicalJson(referenceEvent)

        assertThat(json).doesNotContain("nfcProof")
        assertThat(json).doesNotContain("\"proof\"")
    }

    @Test
    fun fingerprintIsSixtyFourLowercaseHexCharacters() {
        val fingerprint = EventFingerprint.of(referenceEvent)

        assertThat(fingerprint).hasLength(64)
        assertThat(fingerprint).matches("[0-9a-f]{64}".toPattern())
    }

    @Test
    fun fingerprintMatchesTheGoldenValueForTheReferenceEvent() {
        // Valeur figée : tout changement de forme canonique (ordre des clés, champs par défaut)
        // doit faire échouer ce test plutôt que de dériver silencieusement en base v1.
        assertThat(EventFingerprint.of(referenceEvent))
            .isEqualTo("614f2f10615e552fa05867c08aea28a038a1c487b804d03096d26a2eb0ed1aaa")
    }
}
