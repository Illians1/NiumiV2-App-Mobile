package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.niumi.database.entity.BlockedAppEntity

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_app WHERE sessionId = :sessionId")
    suspend fun forSession(sessionId: String): List<BlockedAppEntity>

    @Query("DELETE FROM blocked_app WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<BlockedAppEntity>)

    /** Remplace la sélection d'une session en une seule sous-transaction (SQLite : savepoint). */
    @Transaction
    suspend fun replaceForSession(
        sessionId: String,
        entities: List<BlockedAppEntity>,
    ) {
        deleteForSession(sessionId)
        insertAll(entities)
    }
}
