package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pointeur vers la session active, ligne unique (SPEC_CORE_KMP §6 : « effacer le pointeur actif »
 * pour `ACTIVATION_FAILED` et `RELEASE_SUCCEEDED » ; SPEC_ANDROID §9.2). Absente des specs en tant
 * que table nommée — déduite du contrat, à assumer explicitement (rapport d'étape).
 */
@Entity(
    tableName = "active_session_pointer",
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
data class ActiveSessionPointerEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val sessionId: String,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
