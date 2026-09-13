package com.niumi.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entrée du journal technique borné à 200 (SPEC_ANDROID §7.2, §17). Aucune clé étrangère vers
 * `alarm_session` : le journal doit pouvoir consigner un `sessionId` inconnu ou survivre à la
 * suppression d'une session. `detailsJson` est filtré à l'écriture par `RoomTechnicalEventLog`
 * (liste blanche de clés) : jamais de texte d'accessibilité, de hash de token ou d'identifiant
 * matériel (§16).
 *
 * [deviceModel], [androidVersion] et [appVersion] sont les champs de contexte de §17, ajoutés en
 * v2 de la base (étape 16). Leur valeur par défaut `""` est celle que la migration donne aux
 * lignes écrites en v1, qui ne les portaient pas : une chaîne vide s'affiche comme « inconnu »
 * dans l'export plutôt que de faire échouer la lecture d'un journal antérieur.
 *
 * `@ColumnInfo(defaultValue = "''")` n'est pas décoratif : SQLite exige une valeur par défaut pour
 * ajouter une colonne `NOT NULL` à une table peuplée, donc `MIGRATION_1_2` en pose une. Sans cette
 * annotation, le schéma attendu par Room n'en déclarerait aucune et `validateMigration` échouerait
 * sur cette seule différence.
 */
@Entity(tableName = "technical_event")
data class TechnicalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String?,
    val type: String,
    val createdAtEpochMillis: Long,
    val detailsJson: String?,
    @ColumnInfo(defaultValue = "''") val deviceModel: String = "",
    @ColumnInfo(defaultValue = "''") val androidVersion: String = "",
    @ColumnInfo(defaultValue = "''") val appVersion: String = "",
)
