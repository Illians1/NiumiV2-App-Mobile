package com.niumi.database.mapping

import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.entity.PairedBoxEntity

/**
 * Aller-retour `PairedBoxCredentialDto ↔ PairedBoxEntity`. Attention au nom de colonne :
 * `PairedBoxEntity.tokenSha256` porte le même contenu que `PairedBoxCredentialDto.tokenSha256Hex`
 * (SPEC_CORE_KMP §9.2 : SHA-256 hexadécimal des 16 octets décodés du token).
 */
fun PairedBoxCredentialDto.toEntity(pairedAtEpochMillis: Long): PairedBoxEntity =
    PairedBoxEntity(
        boxId = boxId,
        protocolVersion = protocolVersion,
        tokenSha256 = tokenSha256Hex,
        pairedAtEpochMillis = pairedAtEpochMillis,
    )

fun PairedBoxEntity.toDomain(): PairedBoxCredentialDto =
    PairedBoxCredentialDto(
        protocolVersion = protocolVersion,
        boxId = boxId,
        tokenSha256Hex = tokenSha256,
    )
