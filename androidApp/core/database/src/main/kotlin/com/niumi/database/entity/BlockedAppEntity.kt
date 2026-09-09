package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Application bloquée d'une session, PK composée (SPEC_ANDROID §7.2). */
@Entity(
    tableName = "blocked_app",
    primaryKeys = ["sessionId", "packageName"],
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
data class BlockedAppEntity(
    val sessionId: String,
    val packageName: String,
    val displayNameSnapshot: String,
)
