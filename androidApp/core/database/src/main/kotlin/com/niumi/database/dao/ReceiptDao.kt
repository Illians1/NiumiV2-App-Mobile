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

    // IGNORE, réservé à la fusion Direct Boot → Room (SPEC_ANDROID §9.3, étape 19). Un reçu déjà
    // présent y est la situation normale et non un conflit : la projection Direct Boot est réécrite
    // depuis Room après chaque décision prise déverrouillé, donc elle reporte des reçus que Room
    // possède déjà. Seuls les reçus produits pendant la fenêtre Direct Boot sont réellement
    // nouveaux. `insert` garde son ABORT, qui reste le signal `EVENT_ID_CONFLICT` du coordinateur.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringDuplicates(entities: List<SessionEventReceiptEntity>)

    @Query("SELECT * FROM session_event_receipt WHERE eventId = :eventId")
    suspend fun findByEventId(eventId: String): SessionEventReceiptEntity?

    // Projection Direct Boot (SPEC_ANDROID §7.3, `eventReceipts`) : ordre d'insertion, pas de tri
    // explicite requis par le contrat, l'ordre naturel des lignes (rowid croissant) suffit.
    @Query("SELECT * FROM session_event_receipt WHERE sessionId = :sessionId")
    suspend fun forSession(sessionId: String): List<SessionEventReceiptEntity>
}
