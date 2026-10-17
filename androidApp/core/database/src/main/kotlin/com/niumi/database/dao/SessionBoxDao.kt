package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.database.entity.SessionBoxEntity

/** Liste des boîtiers figée dans une session, par position (SPEC_ANDROID §7.2, Lot 8). */
@Dao
interface SessionBoxDao {
    @Query("SELECT * FROM session_box WHERE sessionId = :sessionId ORDER BY position ASC")
    suspend fun forSession(sessionId: String): List<SessionBoxEntity>

    @Query("DELETE FROM session_box WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SessionBoxEntity>)
}
