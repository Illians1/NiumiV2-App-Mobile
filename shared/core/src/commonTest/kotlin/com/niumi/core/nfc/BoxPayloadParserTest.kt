package com.niumi.core.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Un cas par contrainte de SPEC_CORE_KMP §9.1 et §17. Ordre de validation figé (le premier
 * contrôle qui échoue détermine le statut) — voir le tableau du rapport d'étape.
 */
class BoxPayloadParserTest {
    private val canonicalBoxId = "550e8400-e29b-41d4-a716-446655440000"

    // Token de 22 caractères Base64 URL sans padding, 16 octets, dernier caractère "A" (valide).
    private val canonicalToken = "AAAAAAAAAAAAAAAAAAAAAA"

    private fun uri(
        boxId: String = canonicalBoxId,
        token: String = canonicalToken,
    ) = "niumi://box/v1/$boxId?token=$token"

    @Test
    fun canonicalPayloadIsValid() {
        val result = BoxPayloadParser.parse(uri())
        assertEquals(BoxPayloadStatus.VALID, result.status)
        val payload = assertNotNull(result.payload)
        assertEquals(1, payload.protocolVersion)
        assertEquals(canonicalBoxId, payload.boxId)
        assertEquals(16, payload.tokenBytes.size)
    }

    @Test
    fun uppercaseSchemeIsUnsupported() {
        assertEquals(
            BoxPayloadStatus.UNSUPPORTED_SCHEME,
            BoxPayloadParser.parse("NIUMI://box/v1/$canonicalBoxId?token=$canonicalToken").status,
        )
    }

    @Test
    fun mixedCaseSchemeIsUnsupported() {
        assertEquals(
            BoxPayloadStatus.UNSUPPORTED_SCHEME,
            BoxPayloadParser.parse("Niumi://box/v1/$canonicalBoxId?token=$canonicalToken").status,
        )
    }

    @Test
    fun uppercaseHostIsUnsupported() {
        assertEquals(
            BoxPayloadStatus.UNSUPPORTED_HOST,
            BoxPayloadParser.parse("niumi://Box/v1/$canonicalBoxId?token=$canonicalToken").status,
        )
    }

    @Test
    fun unknownVersionIsUnsupported() {
        assertEquals(
            BoxPayloadStatus.UNSUPPORTED_VERSION,
            BoxPayloadParser.parse("niumi://box/v2/$canonicalBoxId?token=$canonicalToken").status,
        )
    }

    @Test
    fun uppercaseBoxIdIsInvalid() {
        assertEquals(
            BoxPayloadStatus.INVALID_BOX_ID,
            BoxPayloadParser.parse(uri(boxId = canonicalBoxId.uppercase())).status,
        )
    }

    @Test
    fun boxIdWithoutDashesIsInvalid() {
        assertEquals(
            BoxPayloadStatus.INVALID_BOX_ID,
            BoxPayloadParser.parse(uri(boxId = canonicalBoxId.replace("-", ""))).status,
        )
    }

    @Test
    fun boxIdWithWrongLengthIsInvalid() {
        // 35 caractères au lieu de 36.
        val truncated = canonicalBoxId.dropLast(1)
        assertEquals(BoxPayloadStatus.INVALID_BOX_ID, BoxPayloadParser.parse(uri(boxId = truncated)).status)
    }

    @Test
    fun emptyQueryIsMissingToken() {
        assertEquals(BoxPayloadStatus.MISSING_TOKEN, BoxPayloadParser.parse("niumi://box/v1/$canonicalBoxId?").status)
    }

    @Test
    fun noQueryAtAllIsMissingToken() {
        assertEquals(BoxPayloadStatus.MISSING_TOKEN, BoxPayloadParser.parse("niumi://box/v1/$canonicalBoxId").status)
    }

    @Test
    fun duplicatedTokenParamIsUnexpectedComponent() {
        // Second valeur volontairement courte : seul le nombre de paramètres est testé ici : un
        // second "token=$canonicalToken" complet dépasserait les 96 octets et ferait dominer
        // PAYLOAD_TOO_LONG (couvert séparément par payloadTooLongWinsOverEveryOtherViolation).
        assertEquals(
            BoxPayloadStatus.UNEXPECTED_COMPONENT,
            BoxPayloadParser.parse("niumi://box/v1/$canonicalBoxId?token=$canonicalToken&token=x").status,
        )
    }

    @Test
    fun tokenTooShortIsInvalid() {
        assertEquals(
            BoxPayloadStatus.INVALID_TOKEN,
            BoxPayloadParser.parse(uri(token = canonicalToken.dropLast(1))).status,
        )
    }

    @Test
    fun tokenTooLongIsInvalid() {
        assertEquals(BoxPayloadStatus.INVALID_TOKEN, BoxPayloadParser.parse(uri(token = canonicalToken + "A")).status)
    }

    @Test
    fun tokenWithPaddingIsInvalid() {
        assertEquals(
            BoxPayloadStatus.INVALID_TOKEN,
            BoxPayloadParser.parse(uri(token = canonicalToken.dropLast(1) + "=")).status,
        )
    }

    @Test
    fun tokenWithPlusIsInvalid() {
        assertEquals(
            BoxPayloadStatus.INVALID_TOKEN,
            BoxPayloadParser.parse(uri(token = canonicalToken.dropLast(1) + "+")).status,
        )
    }

    @Test
    fun tokenWithSlashIsInvalid() {
        assertEquals(
            BoxPayloadStatus.INVALID_TOKEN,
            BoxPayloadParser.parse(uri(token = canonicalToken.dropLast(1) + "/")).status,
        )
    }

    @Test
    fun tokenWithNonZeroPaddingBitsIsInvalid() {
        // "B" en dernière position code des bits de bourrage non nuls (hors A/Q/g/w).
        assertEquals(
            BoxPayloadStatus.INVALID_TOKEN,
            BoxPayloadParser.parse(uri(token = canonicalToken.dropLast(1) + "B")).status,
        )
    }

    @Test
    fun fragmentIsUnexpectedComponent() {
        assertEquals(BoxPayloadStatus.UNEXPECTED_COMPONENT, BoxPayloadParser.parse(uri() + "#frag").status)
    }

    @Test
    fun userInfoIsUnexpectedComponent() {
        assertEquals(
            BoxPayloadStatus.UNEXPECTED_COMPONENT,
            BoxPayloadParser.parse("niumi://user@box/v1/$canonicalBoxId?token=$canonicalToken").status,
        )
    }

    @Test
    fun portIsUnexpectedComponent() {
        assertEquals(
            BoxPayloadStatus.UNEXPECTED_COMPONENT,
            BoxPayloadParser.parse("niumi://box:443/v1/$canonicalBoxId?token=$canonicalToken").status,
        )
    }

    @Test
    fun extraPathSegmentIsUnexpectedComponent() {
        assertEquals(
            BoxPayloadStatus.UNEXPECTED_COMPONENT,
            BoxPayloadParser.parse("niumi://box/v1/$canonicalBoxId/x?token=$canonicalToken").status,
        )
    }

    @Test
    fun extraQueryParamIsUnexpectedComponent() {
        assertEquals(BoxPayloadStatus.UNEXPECTED_COMPONENT, BoxPayloadParser.parse(uri() + "&a=b").status)
    }

    @Test
    fun percentEncodingIsUnexpectedComponent() {
        assertEquals(
            BoxPayloadStatus.UNEXPECTED_COMPONENT,
            BoxPayloadParser.parse("niumi://box/v1/$canonicalBoxId?token=%2F").status,
        )
    }

    @Test
    fun payloadOver96BytesIsTooLong() {
        val hugeBoxId = canonicalBoxId + "0".repeat(100)
        val huge = uri(boxId = hugeBoxId)
        assertEquals(BoxPayloadStatus.PAYLOAD_TOO_LONG, BoxPayloadParser.parse(huge).status)
    }

    @Test
    fun payloadTooLongWinsOverEveryOtherViolation() {
        // Schéma invalide ET trop long : PAYLOAD_TOO_LONG doit gagner (contrôlé en premier).
        val huge = "NIUMI://box/v1/$canonicalBoxId?token=$canonicalToken" + "&x=".repeat(40)
        assertEquals(BoxPayloadStatus.PAYLOAD_TOO_LONG, BoxPayloadParser.parse(huge).status)
    }

    @Test
    fun emptyStringIsMalformed() {
        assertEquals(BoxPayloadStatus.MALFORMED_URI, BoxPayloadParser.parse("").status)
    }

    @Test
    fun singleNullByteIsMalformed() {
        assertEquals(BoxPayloadStatus.MALFORMED_URI, BoxPayloadParser.parse("\u0000").status)
    }

    @Test
    fun controlCharacterEmbeddedInOtherwiseValidUriIsMalformed() {
        val withControlChar = "niumi://box/v1/$canonicalBoxId?token=$canonicalToken\u0001"
        assertEquals(BoxPayloadStatus.MALFORMED_URI, BoxPayloadParser.parse(withControlChar).status)
    }

    @Test
    fun schemeSeparatorAloneIsMalformed() {
        assertEquals(BoxPayloadStatus.MALFORMED_URI, BoxPayloadParser.parse("://").status)
    }

    @Test
    fun missingSchemeSeparatorIsMalformed() {
        assertEquals(BoxPayloadStatus.MALFORMED_URI, BoxPayloadParser.parse("niumi-box-v1").status)
    }

    @Test
    fun validResultNeverKeepsPayloadOnFailure() {
        val result = BoxPayloadParser.parse("niumi://box/v1/$canonicalBoxId")
        assertNull(result.payload)
    }
}
