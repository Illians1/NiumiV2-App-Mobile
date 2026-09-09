package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.niumi.database.entity.TechnicalEventEntity

@Dao
interface TechnicalEventDao {
    @Insert
    suspend fun insert(entity: TechnicalEventEntity)

    // Tri chronologique (`createdAtEpochMillis`), `id` seulement en départage : l'horodatage est
    // capturé au moment de l'appel à `log()`, l'insertion étant asynchrone, deux écritures peuvent
    // atteindre la base dans un ordre différent de celui des appels. Trier par `id` seul ferait
    // alors mentir la chronologie affichée et purgerait le mauvais événement.
    @Query("SELECT * FROM technical_event ORDER BY createdAtEpochMillis DESC, id DESC LIMIT :limit")
    suspend fun mostRecent(limit: Int): List<TechnicalEventEntity>

    @Query(
        "DELETE FROM technical_event WHERE id NOT IN " +
            "(SELECT id FROM technical_event ORDER BY createdAtEpochMillis DESC, id DESC LIMIT :limit)",
    )
    suspend fun purgeBeyond(limit: Int)

    /** Insertion et purge dans la même transaction (SPEC_ANDROID §7.2 : journal borné à 200). */
    @Transaction
    suspend fun insertAndPurge(
        entity: TechnicalEventEntity,
        limit: Int,
    ) {
        insert(entity)
        purgeBeyond(limit)
    }
}
