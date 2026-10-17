package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Liste des boîtiers figée dans une session à l'activation (SPEC_ANDROID §7.2, Lot 8), sur le
 * modèle exact de [BlockedAppEntity] : `RoomSessionStore.commitDecision` l'écrit à la création,
 * jamais à une décision suivante — c'est le figement imposé par SPEC_CORE_KMP §10. `tokenSha256`
 * reprend le nom de colonne historique d'`alarm_session` plutôt que celui de [PairedBoxEntity]
 * (`tokenSha256`), pour la même raison documentée là : pas de valeur ajoutée à distinguer un nom
 * de colonne propre à une table figée.
 */
@Entity(
    tableName = "session_box",
    primaryKeys = ["sessionId", "boxId"],
    foreignKeys = [
        ForeignKey(
            entity = AlarmSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SessionBoxEntity(
    val sessionId: String,
    val boxId: String,
    val tokenSha256: String,
    val name: String,
    val position: Int,
)
