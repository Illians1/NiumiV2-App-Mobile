package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.database.entity.PairedBoxEntity

/**
 * Jusqu'à cinq boîtiers associés, nommés, ordonnés par position (SPEC_ANDROID §11.1, Lot 8).
 * Le refus d'un doublon ou d'un sixième boîtier se fait au dépôt
 * ([com.niumi.database.pairing.RoomPairedBoxStore]), pas ici : ce DAO n'est que la persistance.
 */
@Dao
interface PairedBoxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PairedBoxEntity)

    @Query("SELECT * FROM paired_box WHERE boxId = :boxId")
    suspend fun findById(boxId: String): PairedBoxEntity?

    @Query("SELECT * FROM paired_box ORDER BY position ASC")
    suspend fun all(): List<PairedBoxEntity>

    @Query("SELECT COUNT(*) FROM paired_box")
    suspend fun count(): Int

    @Query("UPDATE paired_box SET name = :name WHERE boxId = :boxId")
    suspend fun rename(
        boxId: String,
        name: String,
    )

    @Query("DELETE FROM paired_box WHERE boxId = :boxId")
    suspend fun delete(boxId: String)
}
