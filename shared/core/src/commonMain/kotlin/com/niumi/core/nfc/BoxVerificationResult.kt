package com.niumi.core.nfc

/**
 * Résultat de la comparaison entre un payload scanné et la liste des Niumi Spots figés dans la
 * session (SPEC_CORE_KMP §9.2, contrat 1.4). `BOX_MISMATCH` et `TOKEN_MISMATCH` ont disparu avec
 * le Niumi Spot unique : distinguer « bon identifiant, mauvais token » n'avait de sens que face à un
 * seul credential, et c'est une information de moins qui sort du module.
 */
public enum class BoxVerificationStatus {
    MATCH,
    NO_PAIRED_BOX_MATCH,
    UNSUPPORTED_VERSION,
}

/**
 * [proof] n'est non nul que si [status] vaut [BoxVerificationStatus.MATCH] **et** qu'un
 * [NfcVerificationContext] a été fourni à [BoxVerifier.verify]. Elle porte le `boxId` du
 * credential reconnu. Un refus ne porte jamais de preuve, quel que soit le contexte.
 */
public data class BoxVerificationResult(
    val status: BoxVerificationStatus,
    val proof: NfcVerificationProof?,
)
