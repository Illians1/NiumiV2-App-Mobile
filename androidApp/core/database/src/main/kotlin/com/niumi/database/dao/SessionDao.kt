package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.niumi.database.entity.AlarmSessionEntity

@Dao
interface SessionDao {
    @Query("SELECT * FROM alarm_session WHERE id = :sessionId")
    suspend fun findById(sessionId: String): AlarmSessionEntity?

    /**
     * `@Upsert` et non `@Insert(onConflict = REPLACE)` : en SQLite, `INSERT OR REPLACE` est un
     * DELETE suivi d'un INSERT. Il déclenchait les `ForeignKey(onDelete = CASCADE)` des cinq
     * tables enfants (`session_event_receipt`, `session_effect_outbox`, `session_incident`,
     * `blocked_app`, `active_session_pointer`) et effaçait donc tout l'historique de la session à
     * chaque transition d'état : registre d'idempotence réduit au dernier événement (SPEC_CORE_KMP
     * §6.1), effets non rejoués des révisions antérieures, incidents perdus (SPEC_ANDROID §7.1).
     * `@Upsert` met la ligne à jour au lieu de la recréer. Régression couverte par
     * `RoomSessionStoreHistoryTest`, trouvée sur appareil à l'étape 11.
     */
    @Upsert
    suspend fun upsert(entity: AlarmSessionEntity)
}
