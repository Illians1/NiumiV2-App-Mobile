package com.niumi.core.nfc

/**
 * Contenu décodé d'un tag NFC Niumi valide (SPEC_CORE_KMP §9.1) : `niumi://box/v1/{boxId}?token={token}`.
 * [tokenBytes] contient exactement 16 octets décodés du token, jamais leur forme encodée.
 */
public class BoxPayload(
    public val protocolVersion: Int,
    public val boxId: String,
    public val tokenBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoxPayload) return false
        return protocolVersion == other.protocolVersion &&
            boxId == other.boxId &&
            tokenBytes.contentEquals(other.tokenBytes)
    }

    override fun hashCode(): Int {
        var result = protocolVersion
        result = 31 * result + boxId.hashCode()
        result = 31 * result + tokenBytes.contentHashCode()
        return result
    }

    override fun toString(): String =
        "BoxPayload(protocolVersion=$protocolVersion, boxId=$boxId, tokenBytes=<redacted>)"
}
