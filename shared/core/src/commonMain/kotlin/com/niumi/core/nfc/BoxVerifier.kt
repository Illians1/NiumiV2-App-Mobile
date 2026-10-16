package com.niumi.core.nfc

/**
 * Compare un payload NFC scanné à la liste des boîtiers figés dans la session (SPEC_CORE_KMP
 * §9.2, §6, contrat 1.4) : `MATCH` au premier credential dont le `boxId` est égal **et** dont
 * l'empreinte correspond, `NO_PAIRED_BOX_MATCH` sinon, liste vide comprise. Le `boxId` n'est pas
 * secret et se compare normalement ; l'empreinte du token se compare en temps constant.
 */
public object BoxVerifier {
    private const val SUPPORTED_PROTOCOL_VERSION = 1
    private const val HEX_RADIX = 16
    private const val NIBBLE_BITS = 4
    private const val SHA256_HEX_LENGTH = 64

    public fun verify(
        payload: BoxPayload,
        credentials: List<PairedBoxCredential>,
        context: NfcVerificationContext?,
    ): BoxVerificationResult {
        val supported = payload.protocolVersion == SUPPORTED_PROTOCOL_VERSION
        // La version est jugée avant toute comparaison : un payload d'une autre version ne doit
        // pas « correspondre » à un credential, même à identifiant et empreinte égaux.
        val matched = if (supported) matchingCredential(payload, credentials) else null
        val status =
            when {
                !supported -> BoxVerificationStatus.UNSUPPORTED_VERSION
                matched == null -> BoxVerificationStatus.NO_PAIRED_BOX_MATCH
                else -> BoxVerificationStatus.MATCH
            }
        val proof =
            if (matched != null && context != null) {
                NfcVerificationProof(
                    boxId = matched.boxId,
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

    /** Premier credential dont l'identifiant est égal **et** dont l'empreinte correspond. */
    private fun matchingCredential(
        payload: BoxPayload,
        credentials: List<PairedBoxCredential>,
    ): PairedBoxCredential? =
        credentials.firstOrNull { credential ->
            credential.boxId == payload.boxId && tokenMatches(payload, credential)
        }

    // Un `tokenSha256Hex` corrompu peut provenir d'un stockage persistant altéré (DataStore,
    // Room). SPEC_CORE_KMP §14 interdit à toute exception de traverser la frontière : une forme
    // non canonique (longueur incorrecte ou caractère hors `0-9a-f` minuscule) est un credential
    // qui ne correspond pas — les suivants de la liste sont examinés —, jamais un crash.
    private fun tokenMatches(
        payload: BoxPayload,
        credential: PairedBoxCredential,
    ): Boolean {
        if (!isCanonicalSha256Hex(credential.tokenSha256Hex)) return false
        val scannedHash = Sha256.hash(payload.tokenBytes)
        val expectedHash = hexToBytes(credential.tokenSha256Hex)
        return ConstantTime.equals(scannedHash, expectedHash)
    }

    private fun isCanonicalSha256Hex(hex: String): Boolean =
        hex.length == SHA256_HEX_LENGTH && hex.all { it in '0'..'9' || it in 'a'..'f' }

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
