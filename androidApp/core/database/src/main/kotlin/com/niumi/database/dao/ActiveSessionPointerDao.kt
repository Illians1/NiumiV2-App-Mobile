package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.database.entity.ActiveSessionPointerEntity

@Dao
interface ActiveSessionPointerDao {
    @Query("SELECT * FROM active_session_pointer WHERE id = ${ActiveSessionPointerEntity.SINGLETON_ID}")
    suspend fun current(): ActiveSessionPointerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: ActiveSessionPointerEntity)

    /** Idempotent : ne retire jamais le pointeur d'une autre session que [sessionId]. */
    @Query(
        "DELETE FROM active_session_pointer WHERE id = ${ActiveSessionPointerEntity.SINGLETON_ID} " +
            "AND sessionId = :sessionId",
    )
    suspend fun clear(sessionId: String)
}
