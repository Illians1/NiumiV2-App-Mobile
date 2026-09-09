package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.database.entity.PairedBoxEntity

/** Consommé à l'étape 13 (association du boîtier) ; livré ici pour fixer le schéma v1. */
@Dao
interface PairedBoxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PairedBoxEntity)

    @Query("SELECT * FROM paired_box WHERE boxId = :boxId")
    suspend fun findById(boxId: String): PairedBoxEntity?
}
