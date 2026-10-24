package com.niumi.core.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * SPEC_CORE_KMP §9.2 (contrat 1.4) : la vérification compare le payload à chaque credential de la
 * liste figée dans la session, et répond `MATCH` au premier qui correspond — identifiant **et**
 * empreinte —, `NO_PAIRED_BOX_MATCH` sinon, liste vide comprise.
 */
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

    /** Cinq Niumi Spots distincts, tous différents de [boxId] / [tokenBytes]. */
    private fun fiveOtherCredentials(): List<PairedBoxCredential> =
        (1..5).map { index ->
            credentialFor(
                boxId = "${index}60e8400-e29b-41d4-a716-446655440000",
                tokenBytes = ByteArray(16) { (it + index + 10).toByte() },
            )
        }

    private val payload = BoxPayload(protocolVersion = 1, boxId = boxId, tokenBytes = tokenBytes)

    @Test
    fun anEmptyListYieldsNoPairedBoxMatchWithoutProof() {
        val result = BoxVerifier.verify(payload, emptyList(), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun aSingleMatchingCredentialYieldsMatchAndTheProofCarriesItsBoxId() {
        val result = BoxVerifier.verify(payload, listOf(credentialFor(boxId, tokenBytes)), context)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        val proof = assertNotNull(result.proof)
        assertEquals(boxId, proof.boxId)
        assertEquals(context.sessionId, proof.sessionId)
        assertEquals(context.eventId, proof.eventId)
        assertEquals(context.expectedRevision, proof.expectedRevision)
        assertEquals(context.occurredAtEpochMillis, proof.verifiedAtEpochMillis)
    }

    @Test
    fun withoutContextAMatchCarriesNoProof() {
        val result = BoxVerifier.verify(payload, listOf(credentialFor(boxId, tokenBytes)), context = null)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun aSingleCredentialWithAnotherBoxIdYieldsNoPairedBoxMatch() {
        val result = BoxVerifier.verify(payload, listOf(credentialFor(otherBoxId, tokenBytes)), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun aSingleCredentialWithAnotherTokenYieldsNoPairedBoxMatch() {
        val result = BoxVerifier.verify(payload, listOf(credentialFor(boxId, otherTokenBytes)), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun theMatchingCredentialIsFoundInFirstPosition() {
        val credentials = listOf(credentialFor(boxId, tokenBytes)) + fiveOtherCredentials().take(4)

        val result = BoxVerifier.verify(payload, credentials, context)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertEquals(boxId, assertNotNull(result.proof).boxId)
    }

    @Test
    fun theMatchingCredentialIsFoundInLastPosition() {
        val credentials = fiveOtherCredentials().take(4) + credentialFor(boxId, tokenBytes)

        val result = BoxVerifier.verify(payload, credentials, context)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertEquals(boxId, assertNotNull(result.proof).boxId)
    }

    @Test
    fun fiveCredentialsNoneMatchingYieldNoPairedBoxMatch() {
        val result = BoxVerifier.verify(payload, fiveOtherCredentials(), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun aKnownBoxIdWithTheWrongTokenAmongOthersIsNotAMatch() {
        val credentials =
            fiveOtherCredentials().take(2) + credentialFor(boxId, otherTokenBytes) + fiveOtherCredentials().drop(3)

        val result = BoxVerifier.verify(payload, credentials, context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun unsupportedProtocolVersionYieldsUnsupportedVersionEvenWithAMatchingCredential() {
        val unsupported = BoxPayload(protocolVersion = 2, boxId = boxId, tokenBytes = tokenBytes)

        val result = BoxVerifier.verify(unsupported, listOf(credentialFor(boxId, tokenBytes)), context)

        assertEquals(BoxVerificationStatus.UNSUPPORTED_VERSION, result.status)
        assertNull(result.proof)
    }

    // SPEC_CORE_KMP §14 : aucune exception ne doit traverser la frontière. Un `tokenSha256Hex`
    // corrompu (lu depuis un stockage persistant altéré) ne doit jamais faire lancer `verify` :
    // ce credential ne correspond pas, et les suivants sont examinés.
    @Test
    fun aNonCanonicalHashInTheListIsSkippedWithoutThrowing() {
        val corrupt = PairedBoxCredential(protocolVersion = 1, boxId = boxId, tokenSha256Hex = "abcd")
        val credentials = listOf(corrupt, credentialFor(boxId, tokenBytes))

        val result = BoxVerifier.verify(payload, credentials, context)

        assertEquals(BoxVerificationStatus.MATCH, result.status)
        assertEquals(boxId, assertNotNull(result.proof).boxId)
    }

    @Test
    fun emptyStoredHashAloneYieldsNoPairedBoxMatchWithoutThrowing() {
        val credential = PairedBoxCredential(protocolVersion = 1, boxId = boxId, tokenSha256Hex = "")

        val result = BoxVerifier.verify(payload, listOf(credential), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun nonHexCharacterInStoredHashYieldsNoPairedBoxMatchWithoutThrowing() {
        val credential = PairedBoxCredential(protocolVersion = 1, boxId = boxId, tokenSha256Hex = "z".repeat(64))

        val result = BoxVerifier.verify(payload, listOf(credential), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }

    @Test
    fun uppercaseStoredHashYieldsNoPairedBoxMatchWithoutThrowing() {
        val validHash = Sha256.hexOf(Sha256.hash(tokenBytes))
        val credential = PairedBoxCredential(protocolVersion = 1, boxId = boxId, tokenSha256Hex = validHash.uppercase())

        val result = BoxVerifier.verify(payload, listOf(credential), context)

        assertEquals(BoxVerificationStatus.NO_PAIRED_BOX_MATCH, result.status)
        assertNull(result.proof)
    }
}
