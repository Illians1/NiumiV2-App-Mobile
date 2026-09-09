package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto

/** Incident survenu après l'armement d'une session (SPEC_ANDROID §7.2, §7.1). */
@Entity(
    tableName = "session_incident",
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
data class SessionIncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val code: String,
    val severity: IncidentSeverityDto,
    val occurredAtEpochMillis: Long,
    val platform: PlatformDto,
)
