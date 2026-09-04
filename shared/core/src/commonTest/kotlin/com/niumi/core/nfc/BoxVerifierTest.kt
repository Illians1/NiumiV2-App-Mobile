package com.niumi.core.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BoxVerifierTest {
    private val boxId = "550e8400-e29b-41d4-a716-446655440000"
    private val otherBoxId = "660e8400-e29b-41d4-a716-446655440000"
    private val tokenBytes = ByteArray(16) { it.toByte() }
    private val otherTokenBytes = ByteArray(16) { (it + 1).toByte() }

    private val context =
        NfcVerificationContext(
            sessionId = "session-1",
            eventId = "event-1",
            expectedRevision = 3L,
            occurredAtEpochMillis = 1_000L,
        )

    private fun credentialFor(
        boxId: String,
        tokenBytes: ByteArray,
        protocolVersion: Int = 1,
    ) = PairedBoxCredential(
        protocolVersion = protocolVersion,
        boxId = boxId,
        tokenSha256Hex = Sha256.hexOf(Sha256.hash(tokenBytes)),
    )

    @Test
    fun matchingBoxAndTokenWithoutContextYieldsMatchAndNoProof() {
        val payload = BoxPayload(protocolVersion = 1, boxId = boxId, tokenBytes = tokenBytes)
        val credential = credentialFor(boxId, tokenBytes)

        val result = BoxVerifier.verify(payload, credential, context = null)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun matchingBoxAndTokenWithContextYieldsProofMirroringContext() {
        val payload = BoxPayload(protocolVersion = 1, boxId = boxId, tokenBytes = tokenBytes)
        val credential = credentialFor(boxId, tokenBytes)

        val result = BoxVerifier.verify(payload, credential, context)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        val proof = assertNotNull(result.proof)
        assertEquals(boxId, proof.boxId)
        assertEquals(context.sessionId, proof.sessionId)
        assertEquals(context.eventId, proof.eventId)
        assertEquals(context.expectedRevision, proof.expectedRevision)
        assertEquals(context.occurredAtEpochMillis, proof.verifiedAtEpochMillis)
    }

    @Test
    fun differentBoxIdYieldsBoxMismatchWithoutProof() {
        val payload = BoxPayload(protocolVersion = 1, boxId = otherBoxId, tokenBytes = tokenBytes)
        val credential = credentialFor(boxId, tokenBytes)

        val result = BoxVerifier.verify(payload, credential, context)

        assertEquals(BoxVerificationStatus.BOX_MISMATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun differentTokenYieldsTokenMismatchWithoutProof() {
        val payload = BoxPayload(protocolVersion = 1, boxId = boxId, tokenBytes = otherTokenBytes)
        val credential = credentialFor(boxId, tokenBytes)

        val result = BoxVerifier.verify(payload, credential, context)

        assertEquals(BoxVerificationStatus.TOKEN_MISMATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun unsupportedProtocolVersionYieldsUnsupportedVersionWithoutProof() {
        val payload = BoxPayload(protocolVersion = 2, boxId = boxId, tokenBytes = tokenBytes)
        val credential = credentialFor(boxId, tokenBytes, protocolVersion = 1)

        val result = BoxVerifier.verify(payload, credential, context)

        assertEquals(BoxVerificationStatus.UNSUPPORTED_VERSION, result.status)
        assertNull(result.proof)
    }
}
