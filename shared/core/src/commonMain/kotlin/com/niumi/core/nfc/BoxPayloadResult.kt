package com.niumi.core.nfc

/**
 * Résultats typés du parseur de payload NFC (SPEC_CORE_KMP §9.3). Le premier contrôle qui
 * échoue détermine le statut ; voir l'ordre de validation documenté dans `BoxPayloadParser`.
 */
public enum class BoxPayloadStatus {
    VALID,
    UNSUPPORTED_SCHEME,
    UNSUPPORTED_HOST,
    UNSUPPORTED_VERSION,
    INVALID_BOX_ID,
    MISSING_TOKEN,
    INVALID_TOKEN,
    UNEXPECTED_COMPONENT,
    PAYLOAD_TOO_LONG,
    MALFORMED_URI,
}

/** [payload] n'est non nul que lorsque [status] vaut [BoxPayloadStatus.VALID]. */
public data class BoxPayloadResult(
    val status: BoxPayloadStatus,
    val payload: BoxPayload?,
)
