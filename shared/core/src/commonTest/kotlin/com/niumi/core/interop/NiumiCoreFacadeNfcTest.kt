package com.niumi.core.interop

import com.niumi.core.nfc.BoxPayloadStatus
import com.niumi.core.nfc.BoxVerificationStatus
import com.niumi.core.nfc.Sha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NiumiCoreFacadeNfcTest {
    private val facade = NiumiCoreFacade()
    private val boxId = "550e8400-e29b-41d4-a716-446655440000"
    private val canonicalUri = "niumi://box/v1/$boxId?token=AAAAAAAAAAAAAAAAAAAAAA"

    @Test
    fun parseBoxPayloadRelaysValidStatus() {
        val result = facade.parseBoxPayload(canonicalUri)
        assertEquals(BoxPayloadStatus.VALID, result.status)
        assertNotNull(result.payload)
    }

    @Test
    fun parseBoxPayloadRelaysFailureStatusWithoutThrowing() {
        val result = facade.parseBoxPayload("not-a-niumi-uri")
        assertEquals(BoxPayloadStatus.MALFORMED_URI, result.status)
        assertNull(result.payload)
    }

    @Test
    fun verifyBoxWithContextExposesOpaqueProof() {
        val payload = facade.parseBoxPayload(canonicalUri).payload!!
        val credential =
            PairedBoxCredentialDto(
                protocolVersion = payload.protocolVersion,
                boxId = payload.boxId,
                tokenSha256Hex = Sha256.hexOf(Sha256.hash(payload.tokenBytes)),
            )
        val context =
            NfcVerificationContextDto(
                sessionId = "session-1",
                eventId = "event-1",
                expectedRevision = 1L,
                occurredAtEpochMillis = 1_000L,
            )

        val result = facade.verifyBox(payload, credential, context)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertNotNull(result.proof)
    }

    @Test
    fun verifyBoxWithoutContextNeverExposesProof() {
        val payload = facade.parseBoxPayload(canonicalUri).payload!!
        val credential =
            PairedBoxCredentialDto(
                protocolVersion = payload.protocolVersion,
                boxId = payload.boxId,
                tokenSha256Hex = Sha256.hexOf(Sha256.hash(payload.tokenBytes)),
            )

        val result = facade.verifyBox(payload, credential, context = null)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun verifyBoxMismatchNeverExposesProofEvenWithContext() {
        val payload = facade.parseBoxPayload(canonicalUri).payload!!
        val wrongCredential =
            PairedBoxCredentialDto(
                protocolVersion = payload.protocolVersion,
                boxId = "660e8400-e29b-41d4-a716-446655440000",
                tokenSha256Hex = Sha256.hexOf(Sha256.hash(payload.tokenBytes)),
            )
        val context =
            NfcVerificationContextDto(
                sessionId = "session-1",
                eventId = "event-1",
                expectedRevision = 1L,
                occurredAtEpochMillis = 1_000L,
            )

        val result = facade.verifyBox(payload, wrongCredential, context)

        assertEquals(BoxVerificationStatus.BOX_MISMATCH, result.status)
        assertNull(result.proof)
    }
}
