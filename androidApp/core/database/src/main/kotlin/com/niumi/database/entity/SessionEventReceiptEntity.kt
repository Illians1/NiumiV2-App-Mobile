package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reçu d'un événement appliqué (SPEC_CORE_KMP §6.1, SPEC_ANDROID §7.2). Insertion en `ABORT`
 * (`RoomSessionStore`) : un `eventId` déjà présent avec une empreinte différente est le signal
 * métier `EVENT_ID_CONFLICT`, jamais un `REPLACE` silencieux.
 */
@Entity(
    tableName = "session_event_receipt",
    foreignKeys = [
        ForeignKey(
            entity = AlarmSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SessionEventReceiptEntity(
    @PrimaryKey val eventId: String,
    val sessionId: String,
    val payloadSha256Hex: String,
    val appliedRevision: Long,
    val receivedAtEpochMillis: Long,
)
