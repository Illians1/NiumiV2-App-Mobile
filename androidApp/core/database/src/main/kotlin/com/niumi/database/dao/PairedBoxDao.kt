package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.database.entity.PairedBoxEntity

/**
 * Un seul boîtier associé dans le MVP (SPEC_ANDROID §11.1) : [current] lit la table sans connaître
 * de `boxId`, et [deleteAll] permet à [com.niumi.database.pairing.RoomPairedBoxStore] de retirer
 * l'ancien boîtier avant d'insérer le nouveau, y compris quand le `boxId` change (`upsert` seul ne
 * couvre que le remplacement d'une même clé primaire).
 */
@Dao
interface PairedBoxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PairedBoxEntity)

    @Query("SELECT * FROM paired_box WHERE boxId = :boxId")
    suspend fun findById(boxId: String): PairedBoxEntity?

    @Query("SELECT * FROM paired_box LIMIT 1")
    suspend fun current(): PairedBoxEntity?

    @Query("DELETE FROM paired_box")
    suspend fun deleteAll()
}
