package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Boîtier associé (SPEC_ANDROID §7.2). `tokenSha256` : SHA-256 hexadécimal des 16 octets décodés
 * du token, jamais le token en clair (SPEC_CORE_KMP §9.2, SPEC_ANDROID §11.1).
 */
@Entity(tableName = "paired_box")
data class PairedBoxEntity(
    @PrimaryKey val boxId: String,
    val protocolVersion: Int,
    val tokenSha256: String,
    val pairedAtEpochMillis: Long,
)
