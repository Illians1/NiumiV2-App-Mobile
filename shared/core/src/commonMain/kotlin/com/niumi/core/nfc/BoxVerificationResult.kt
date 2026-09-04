package com.niumi.core.nfc

/** Résultat de la comparaison entre un payload scanné et le boîtier associé. */
public enum class BoxVerificationStatus {
    MATCH,
    BOX_MISMATCH,
    TOKEN_MISMATCH,
    UNSUPPORTED_VERSION,
}

/**
 * [proof] n'est non nul que si [status] vaut [BoxVerificationStatus.MATCH] **et** qu'un
 * [NfcVerificationContext] a été fourni à [BoxVerifier.verify]. Un mismatch ne porte jamais de
 * preuve, quel que soit le contexte.
 */
public data class BoxVerificationResult(
    val status: BoxVerificationStatus,
    val proof: NfcVerificationProof?,
)
