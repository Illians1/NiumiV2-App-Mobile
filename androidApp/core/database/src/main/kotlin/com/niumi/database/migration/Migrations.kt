package com.niumi.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2 (étape 16) : les trois champs de contexte que SPEC_ANDROID §17 exige sur chaque
 * événement du journal technique — modèle de l'appareil, version Android, version de
 * l'application.
 *
 * Migration additive, sur la seule table `technical_event`. Les lignes écrites en v1 ne portent
 * pas ces valeurs et reçoivent `''` : le journal antérieur est conservé plutôt que vidé, un
 * événement sans contexte restant utile au diagnostic. `NOT NULL DEFAULT ''` reproduit exactement
 * ce que Room attend des valeurs par défaut Kotlin de `TechnicalEventEntity`, faute de quoi
 * `validateMigration` échoue sur une différence de schéma.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE technical_event ADD COLUMN deviceModel TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE technical_event ADD COLUMN androidVersion TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE technical_event ADD COLUMN appVersion TEXT NOT NULL DEFAULT ''")
        }
    }
