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
    private val otherBoxId = "660e8400-e29b-41d4-a716-446655440000"
    private val canonicalUri = "niumi://box/v1/$boxId?token=AAAAAAAAAAAAAAAAAAAAAA"

    private val context =
        NfcVerificationContextDto(
            sessionId = "session-1",
            eventId = "event-1",
            expectedRevision = 1L,
            occurredAtEpochMillis = 1_000L,
        )

    private fun credentialOf(
        payload: BoxPayloadDto,
        boxId: String = payload.boxId,
    ) = PairedBoxCredentialDto(
        protocolVersion = payload.protocolVersion,
        boxId = boxId,
        tokenSha256Hex = Sha256.hexOf(Sha256.hash(payload.tokenBytes)),
    )

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

        val result = facade.verifyBox(payload, listOf(credentialOf(payload)), context)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertNotNull(result.proof)
    }

    @Test
    fun verifyBoxWithoutContextNeverExposesProof() {
        val payload = facade.parseBoxPayload(canonicalUri).payload!!

        val result = facade.verifyBox(payload, listOf(credentialOf(payload)), context = null)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun verifyBoxMismatchNeverExposesProofEvenWithContext() {
        val payload = facade.parseBoxPayload(canonicalUri).payload!!

        val result = facade.verifyBox(payload, listOf(credentialOf(payload, boxId = otherBoxId)), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }

    /** Contrat 1.4 : la preuve porte le `boxId` du boîtier reconnu dans la liste, pas le premier. */
    @Test
    fun verifyBoxAgainstAListExposesTheProofOfTheMatchingBox() {
        val payload = facade.parseBoxPayload(canonicalUri).payload!!
        val credentials = listOf(credentialOf(payload, boxId = otherBoxId), credentialOf(payload))

        val result = facade.verifyBox(payload, credentials, context)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertEquals(boxId, assertNotNull(result.proof).boxId)
    }

    @Test
    fun verifyBoxAgainstAnEmptyListNeverExposesProof() {
        val payload = facade.parseBoxPayload(canonicalUri).payload!!

        val result = facade.verifyBox(payload, emptyList(), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }
}
