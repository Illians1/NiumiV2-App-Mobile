package com.niumi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.EffectStatus
import com.niumi.database.entity.SessionEffectOutboxEntity

@Dao
interface OutboxDao {
    // IGNORE : effectId est déterministe (SPEC_CORE_KMP §6), un rejeu de la même décision ne doit
    // pas écraser un statut déjà avancé.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<SessionEffectOutboxEntity>)

    // PENDING et FAILED sont tous deux rejoués (écart au plan MVP, voir ETAPE-09.md) : un effet
    // requis resté FAILED après une interruption doit être rejoué au redémarrage (SPEC_CORE_KMP
    // §6.1), pas seulement un effet encore PENDING.
    @Query(
        "SELECT * FROM session_effect_outbox WHERE sessionId = :sessionId " +
            "AND status IN ('PENDING', 'FAILED') ORDER BY revision ASC, ordinal ASC",
    )
    suspend fun replayable(sessionId: String): List<SessionEffectOutboxEntity>

    // Étape 15 : `replayable` ne suffit pas à la projection de blocage. Elle doit savoir si un
    // effet donné a *réussi* (`SUCCEEDED`/`SATISFIED`), donc lire un statut que `replayable`
    // exclut par construction, et pour un seul `kind`. Ordre décroissant : une reprise peut avoir
    // produit le même `kind` sur plusieurs révisions, seule la plus récente décrit l'état courant.
    @Query(
        "SELECT * FROM session_effect_outbox WHERE sessionId = :sessionId AND kind = :kind " +
            "ORDER BY revision DESC, ordinal DESC",
    )
    suspend fun forSessionAndKind(
        sessionId: String,
        kind: SessionEffectKindDto,
    ): List<SessionEffectOutboxEntity>

    @Query(
        "UPDATE session_effect_outbox SET status = :status, lastError = :error, " +
            "updatedAtEpochMillis = :updatedAtEpochMillis WHERE effectId = :effectId",
    )
    suspend fun updateStatus(
        effectId: String,
        status: EffectStatus,
        error: String?,
        updatedAtEpochMillis: Long,
    )
}
