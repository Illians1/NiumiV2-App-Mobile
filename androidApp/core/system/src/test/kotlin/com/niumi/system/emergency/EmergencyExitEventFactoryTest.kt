package com.niumi.system.emergency

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.EmergencyTranscriptionInputDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.fakes.FakeClock
import com.niumi.system.session.fakes.SequentialIdGenerator
import com.niumi.system.session.fakes.SessionDtoFixtures
import org.junit.Test

private const val NOW = 1_757_000_000_000L

/**
 * `EMERGENCY_EXIT_CONFIRMED` en deux temps (étape 44) : l'identité frappée par le contexte est
 * celle que la preuve lie, et l'événement la reprend telle quelle — sinon `UNEXPECTED_EVENT_PAYLOAD`.
 */
class EmergencyExitEventFactoryTest {
    private val factory = EmergencyExitEventFactory(SequentialIdGenerator(), FakeClock(NOW))
    private val facade = NiumiCoreFacade()

    @Test
    fun theContextCarriesTheSnapshotIdentityAndTheCurrentInstant() {
        val snapshot = SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING, revision = 6)

        val context = factory.verificationContext(snapshot)

        assertThat(context.sessionId).isEqualTo(snapshot.sessionId)
        assertThat(context.expectedRevision).isEqualTo(6)
        assertThat(context.occurredAtEpochMillis).isEqualTo(NOW)
    }

    @Test
    fun theEventReusesTheContextIdentityAndCarriesOnlyTheProof() {
        val context =
            factory.verificationContext(
                SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING, revision = 6),
            )
        val text = facade.emergencyExitTexts().first()
        val proof =
            requireNotNull(
                facade.verifyEmergencyTranscription(EmergencyTranscriptionInputDto(text.id, text.text), context).proof,
            )

        val event = factory.emergencyExitConfirmed(context, proof)

        assertThat(event.kind).isEqualTo(SessionEventKindDto.EMERGENCY_EXIT_CONFIRMED)
        assertThat(event.eventId).isEqualTo(context.eventId)
        assertThat(event.sessionId).isEqualTo(context.sessionId)
        assertThat(event.expectedRevision).isEqualTo(6)
        assertThat(event.occurredAtEpochMillis).isEqualTo(NOW)
        assertThat(event.emergencyProof).isSameInstanceAs(proof)
        assertThat(event.nfcProof).isNull()
        assertThat(event.activationRequest).isNull()
    }
}
