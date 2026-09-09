package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entrée du journal technique borné à 200 (SPEC_ANDROID §7.2, §17). Aucune clé étrangère vers
 * `alarm_session` : le journal doit pouvoir consigner un `sessionId` inconnu ou survivre à la
 * suppression d'une session. `detailsJson` est filtré à l'écriture par `RoomTechnicalEventLog`
 * (liste blanche de clés) : jamais de texte d'accessibilité, de hash de token ou d'identifiant
 * matériel (§16).
 */
@Entity(tableName = "technical_event")
data class TechnicalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String?,
    val type: String,
    val createdAtEpochMillis: Long,
    val detailsJson: String?,
)
