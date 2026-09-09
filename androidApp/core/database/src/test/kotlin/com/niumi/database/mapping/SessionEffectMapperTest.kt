package com.niumi.database.mapping

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.IncidentEffectPayloadDto
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import org.junit.Test

/**
 * `SessionDecisionDto.effects → List<PendingEffect>` (SPEC_CORE_KMP §6, §6.1). La décision testée
 * vient d'un vrai appel à `NiumiCoreFacade.reduce()` plutôt que d'un effet fabriqué à la main : si
 * `:shared:core` change l'ordre des effets ou la formule de `effectId` (`EffectIdFactory`,
 * `internal`, reformulée ici en dur faute d'accès), ce test casse bruyamment au lieu de laisser
 * l'outbox se corrompre en silence.
 */
class SessionEffectMapperTest {
    private val facade = NiumiCoreFacade()
    private val sessionId = "11111111-1111-1111-1111-111111111111"
    private val newRevision = 3L

    private val incidentDto =
        SessionIncidentDto(
            code = IncidentCodes.MISSED_TRIGGER_WINDOW,
            severity = IncidentSeverityDto.DEGRADED,
            occurredAtEpochMillis = 1_700_000_010_000L,
            platform = PlatformDto.ANDROID,
        )

    @Test
    fun ordinalsFollowTheOrderOfTheDecisionEffects() {
        val effects = incidentReportedEffects()

        assertThat(effects.map { it.ordinal }).containsExactly(0, 1).inOrder()
    }

    @Test
    fun effectIdMatchesTheDeterministicFormula() {
        val effects = incidentReportedEffects()

        assertThat(effects[0].effectId)
            .isEqualTo("$sessionId:$newRevision:${SessionEffectKindDto.RECORD_INCIDENT}:0")
        assertThat(effects[1].effectId)
            .isEqualTo("$sessionId:$newRevision:${SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT}:1")
    }

    @Test
    fun effectsWithoutPayloadHaveANullPayloadJson() {
        val effects = incidentReportedEffects()

        assertThat(effects[1].kind).isEqualTo(SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT)
        assertThat(effects[1].payloadJson).isNull()
    }

    @Test
    fun newEffectsStartPendingWithNoError() {
        incidentReportedEffects().forEach { effect ->
            assertThat(effect.status).isEqualTo(EffectStatus.PENDING)
            assertThat(effect.lastError).isNull()
        }
    }

    @Test
    fun incidentPayloadIsPinnedToAShortStableDiscriminant() {
        val effects = incidentReportedEffects()

        assertThat(effects[0].kind).isEqualTo(SessionEffectKindDto.RECORD_INCIDENT)
        assertThat(effects[0].payloadJson).isEqualTo(
            """{"type":"incident","incident":{"code":"MISSED_TRIGGER_WINDOW",""" +
                """"severity":"DEGRADED","occurredAtEpochMillis":1700000010000,"platform":"ANDROID"}}""",
        )
    }

    @Test
    fun incidentPayloadRoundTripsThroughNiumiPersistenceJson() {
        val json = SessionEffectMapper.encodePayload(IncidentEffectPayloadDto(incidentDto))

        val decoded = SessionEffectMapper.decodeIncidentPayload(json)

        assertThat(decoded.incident).isEqualTo(incidentDto)
    }

    @Test
    fun sealedPayloadDiscriminantsAreExactlyTheKnownSet() {
        assertThat(SessionEffectMapper.knownPayloadDiscriminants()).containsExactly("incident")
    }

    private fun incidentReportedEffects(): List<PendingEffect> {
        val armedSnapshot =
            SessionSnapshotDtoFixtures.preparingSnapshot(sessionId, revision = 2).copy(
                state = SessionState.ARMED,
                armedAtEpochMillis = 1_700_000_001_000L,
            )
        val event =
            SessionEventDto(
                eventId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                sessionId = sessionId,
                kind = SessionEventKindDto.INCIDENT_REPORTED,
                occurredAtEpochMillis = 1_700_000_010_000L,
                expectedRevision = 2,
                activationRequest = null,
                failureCode = null,
                incident = incidentDto,
            )
        val decision = facade.reduce(armedSnapshot, event)
        return SessionEffectMapper.run { decision.effects.toPendingEffects() }
    }
}
