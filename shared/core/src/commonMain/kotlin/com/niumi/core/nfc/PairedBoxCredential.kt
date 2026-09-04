package com.niumi.core.nfc

/**
 * Boîtier associé, tel que stocké par la plateforme native (SPEC_CORE_KMP §9.2). Ne contient
 * jamais le token en clair, uniquement l'empreinte SHA-256 de ses 16 octets décodés.
 */
public data class PairedBoxCredential(
    val protocolVersion: Int,
    val boxId: String,
    val tokenSha256Hex: String,
) {
    public companion object {
        /** Dérive le credential d'un [payload] fraîchement associé, pour le stockage natif. */
        public fun fromPayload(payload: BoxPayload): PairedBoxCredential =
            PairedBoxCredential(
                protocolVersion = payload.protocolVersion,
                boxId = payload.boxId,
                tokenSha256Hex = Sha256.hexOf(Sha256.hash(payload.tokenBytes)),
            )
    }
}
