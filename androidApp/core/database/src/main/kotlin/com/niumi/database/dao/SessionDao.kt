package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.database.entity.AlarmSessionEntity

@Dao
interface SessionDao {
    @Query("SELECT * FROM alarm_session WHERE id = :sessionId")
    suspend fun findById(sessionId: String): AlarmSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AlarmSessionEntity)
}
