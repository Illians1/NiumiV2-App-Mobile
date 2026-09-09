package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.niumi.database.entity.SessionIncidentEntity

/**
 * Non consommé par `RoomSessionStore` à cette étape (aucun exécuteur d'effet n'existe encore,
 * étape 11) : entité et DAO livrés maintenant pour fixer le schéma v1, écrits plus tard par
 * l'exécuteur de `RECORD_INCIDENT`.
 */
@Dao
interface IncidentDao {
    @Insert
    suspend fun insert(entity: SessionIncidentEntity)

    @Query("SELECT * FROM session_incident WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun forSession(sessionId: String): List<SessionIncidentEntity>
}
