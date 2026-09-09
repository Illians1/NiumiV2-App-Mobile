package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.EffectStatus

/**
 * Effet en attente ou déjà exécuté (SPEC_CORE_KMP §6.1, §6 ; SPEC_ANDROID §7.2). Insertion en
 * `IGNORE` (`RoomSessionStore`) : `effectId` est déterministe, un rejeu de la même décision ne
 * doit pas écraser un `status` déjà avancé. `ordinal` : voir `SessionEffectMapper` — le DTO
 * `SessionEffectDto` ne le porte pas, il vient de l'index dans `SessionDecisionDto.effects`.
 */
@Entity(
    tableName = "session_effect_outbox",
    foreignKeys = [
        ForeignKey(
            entity = AlarmSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId", "status")],
)
data class SessionEffectOutboxEntity(
    @PrimaryKey val effectId: String,
    val sessionId: String,
    val revision: Long,
    val kind: SessionEffectKindDto,
    val ordinal: Int,
    val payloadJson: String?,
    val status: EffectStatus,
    val lastError: String?,
    val updatedAtEpochMillis: Long,
)
