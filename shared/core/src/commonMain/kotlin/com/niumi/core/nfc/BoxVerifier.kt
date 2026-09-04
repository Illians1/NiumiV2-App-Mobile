package com.niumi.core.nfc

/**
 * Compare un payload NFC scanné au boîtier associé (SPEC_CORE_KMP §9.2, §6). Le `boxId` n'est
 * pas secret et se compare normalement ; l'empreinte du token se compare en temps constant.
 */
public object BoxVerifier {
    private const val SUPPORTED_PROTOCOL_VERSION = 1
    private const val HEX_RADIX = 16
    private const val NIBBLE_BITS = 4

    public fun verify(
        payload: BoxPayload,
        credential: PairedBoxCredential,
        context: NfcVerificationContext?,
    ): BoxVerificationResult {
        val status = statusOf(payload, credential)
        val proof =
            if (status == BoxVerificationStatus.MATCH && context != null) {
                NfcVerificationProof(
                    boxId = payload.boxId,
                    sessionId = context.sessionId,
                    eventId = context.eventId,
                    expectedRevision = context.expectedRevision,
                    verifiedAtEpochMillis = context.occurredAtEpochMillis,
                )
            } else {
                null
            }
        return BoxVerificationResult(status, proof)
    }

    private fun statusOf(
        payload: BoxPayload,
        credential: PairedBoxCredential,
    ): BoxVerificationStatus =
        when {
            payload.protocolVersion != SUPPORTED_PROTOCOL_VERSION -> BoxVerificationStatus.UNSUPPORTED_VERSION
            payload.boxId != credential.boxId -> BoxVerificationStatus.BOX_MISMATCH
            !tokenMatches(payload, credential) -> BoxVerificationStatus.TOKEN_MISMATCH
            else -> BoxVerificationStatus.MATCH
        }

    private fun tokenMatches(
        payload: BoxPayload,
        credential: PairedBoxCredential,
    ): Boolean {
        val scannedHash = Sha256.hash(payload.tokenBytes)
        val expectedHash = hexToBytes(credential.tokenSha256Hex)
        return ConstantTime.equals(scannedHash, expectedHash)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val bytes = ByteArray(hex.length / 2)
        for (i in bytes.indices) {
            val high = hex[i * 2].digitToInt(radix = HEX_RADIX)
            val low = hex[i * 2 + 1].digitToInt(radix = HEX_RADIX)
            bytes[i] = ((high shl NIBBLE_BITS) or low).toByte()
        }
        return bytes
    }
}
