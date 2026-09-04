package com.niumi.core.interop

import com.niumi.core.nfc.BoxPayload
import com.niumi.core.nfc.BoxPayloadResult
import com.niumi.core.nfc.BoxPayloadStatus
import com.niumi.core.nfc.BoxVerificationResult
import com.niumi.core.nfc.BoxVerificationStatus
import com.niumi.core.nfc.NfcVerificationContext
import com.niumi.core.nfc.NfcVerificationProof
import com.niumi.core.nfc.PairedBoxCredential

/**
 * DTO de la frontière `interop` (SPEC_CORE_KMP §14) : data classes simples, sans type de
 * plateforme, sans `Flow` ni `suspend`. Les enums du domaine ([BoxPayloadStatus],
 * [BoxVerificationStatus]) sont réutilisés tels quels plutôt que dupliqués : ce sont déjà des
 * « enums stables », et Swift comme Kotlin/Android les consomment directement. Décision
 * consignée dans `ETAPE-02.md`, valable pour les DTO ajoutés aux étapes suivantes.
 */
public data class BoxPayloadDto(
    val protocolVersion: Int,
    val boxId: String,
    val tokenBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoxPayloadDto) return false
        return protocolVersion == other.protocolVersion && boxId == other.boxId &&
            tokenBytes.contentEquals(other.tokenBytes)
    }

    override fun hashCode(): Int {
        var result = protocolVersion
        result = 31 * result + boxId.hashCode()
        result = 31 * result + tokenBytes.contentHashCode()
        return result
    }
}

public data class BoxPayloadResultDto(
    val status: BoxPayloadStatus,
    val payload: BoxPayloadDto?,
)

public data class PairedBoxCredentialDto(
    val protocolVersion: Int,
    val boxId: String,
    val tokenSha256Hex: String,
) {
    public companion object {
        /** Dérive le credential DTO d'un payload DTO fraîchement associé (SPEC_ANDROID §11.1). */
        public fun fromPayload(payload: BoxPayloadDto): PairedBoxCredentialDto =
            PairedBoxCredential.fromPayload(payload.toDomain()).toDto()
    }
}

public data class NfcVerificationContextDto(
    val sessionId: String,
    val eventId: String,
    val expectedRevision: Long,
    val occurredAtEpochMillis: Long,
)

/**
 * [proof] reste le type opaque [NfcVerificationProof] du domaine : SPEC_CORE_KMP §14 l'expose
 * directement à Swift, sans initialiseur public, jamais stocké. Il n'y a rien de plus simple à en
 * faire un DTO distinct.
 */
public data class BoxVerificationResultDto(
    val status: BoxVerificationStatus,
    val proof: NfcVerificationProof?,
)

internal fun BoxPayloadDto.toDomain(): BoxPayload = BoxPayload(protocolVersion, boxId, tokenBytes)

internal fun BoxPayload.toDto(): BoxPayloadDto = BoxPayloadDto(protocolVersion, boxId, tokenBytes)

internal fun BoxPayloadResult.toDto(): BoxPayloadResultDto = BoxPayloadResultDto(status, payload?.toDto())

internal fun PairedBoxCredentialDto.toDomain(): PairedBoxCredential =
    PairedBoxCredential(protocolVersion, boxId, tokenSha256Hex)

internal fun PairedBoxCredential.toDto(): PairedBoxCredentialDto =
    PairedBoxCredentialDto(protocolVersion, boxId, tokenSha256Hex)

internal fun NfcVerificationContextDto.toDomain(): NfcVerificationContext =
    NfcVerificationContext(sessionId, eventId, expectedRevision, occurredAtEpochMillis)

internal fun BoxVerificationResult.toDto(): BoxVerificationResultDto = BoxVerificationResultDto(status, proof)
