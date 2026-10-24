package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.database.entity.PairedBoxEntity
import kotlinx.coroutines.flow.Flow

/**
 * Jusqu'à dix Niumi Spots associés (contrat KMP 1.5), nommés, ordonnés par position (SPEC_ANDROID §11.1, Lot 8).
 * Le refus d'un doublon ou d'un onzième Niumi Spot se fait au dépôt
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

    /** Même ordre que [all], réémis par Room à chaque écriture dans `paired_box`. */
    @Query("SELECT * FROM paired_box ORDER BY position ASC")
    fun observe(): Flow<List<PairedBoxEntity>>

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
