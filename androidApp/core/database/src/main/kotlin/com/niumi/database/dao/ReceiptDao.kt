package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.database.entity.SessionEventReceiptEntity

@Dao
interface ReceiptDao {
    // ABORT (défaut de la contrainte de clé primaire) : un eventId déjà présent avec une empreinte
    // différente est le signal métier EVENT_ID_CONFLICT (SPEC_CORE_KMP §6.1), jamais un REPLACE.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SessionEventReceiptEntity)

    @Query("SELECT * FROM session_event_receipt WHERE eventId = :eventId")
    suspend fun findByEventId(eventId: String): SessionEventReceiptEntity?

    // Projection Direct Boot (SPEC_ANDROID §7.3, `eventReceipts`) : ordre d'insertion, pas de tri
    // explicite requis par le contrat, l'ordre naturel des lignes (rowid croissant) suffit.
    @Query("SELECT * FROM session_event_receipt WHERE sessionId = :sessionId")
    suspend fun forSession(sessionId: String): List<SessionEventReceiptEntity>
}
