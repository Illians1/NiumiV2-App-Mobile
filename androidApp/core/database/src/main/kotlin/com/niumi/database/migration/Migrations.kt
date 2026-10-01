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

/**
 * v2 → v3 (Lot 6, blocage différé) : les quatre colonnes `blocking*` que SPEC_ANDROID §7.2 ajoute à
 * `alarm_session`, et la montée de `schemaVersion` du snapshot commun à 2 (SPEC_CORE_KMP §7.1).
 *
 * Migration additive, sur la seule table `alarm_session`. Aucune clause `DEFAULT` ici, contrairement
 * à [MIGRATION_1_2] : les quatre colonnes sont **nullables**, et SQLite n'exige une valeur par défaut
 * que pour ajouter une colonne `NOT NULL` à une table peuplée. Room n'en attend donc aucune dans le
 * schéma, et en déclarer une ferait échouer `validateMigration` sur cette seule différence.
 *
 * Les lignes existantes décrivent toutes un blocage immédiat — le choix d'un début différé n'existait
 * pas avant ce lot. Les trois premières colonnes restent donc nulles, et
 * `blockingAppliedAtEpochMillis` reçoit `createdAtEpochMillis` : une session en cours pendant la mise
 * à jour avait bien demandé son blocage dès son activation, et la laisser nulle la ferait relire comme
 * « blocage en attente », ce qui **lèverait le blocage** d'une session active (§12.2).
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE alarm_session ADD COLUMN blockingLocalDate TEXT")
            db.execSQL("ALTER TABLE alarm_session ADD COLUMN blockingLocalTime TEXT")
            db.execSQL("ALTER TABLE alarm_session ADD COLUMN blockingStartsAtEpochMillis INTEGER")
            db.execSQL("ALTER TABLE alarm_session ADD COLUMN blockingAppliedAtEpochMillis INTEGER")
            db.execSQL(
                "UPDATE alarm_session SET blockingAppliedAtEpochMillis = createdAtEpochMillis, schemaVersion = 2",
            )
        }
    }

/**
 * v3 → v4 (Lot 7, sonneries et volume progressif) : la colonne `volumeRampSeconds` de SPEC_ANDROID
 * §7.2, puis la réécriture de la sonnerie retirée de l'APK.
 *
 * Colonne nullable, sans clause `DEFAULT`, pour la même raison que [MIGRATION_2_3]. Les lignes
 * existantes reçoivent `NULL` — volume constant : une session armée avant la mise à jour n'a pas
 * choisi de montée, et on ne lui impose pas un départ presque inaudible. `niumi_alarm` devient
 * `niumi_piano` ([LegacyRingtone]) : sans cette réécriture, la session sonnerait par le repli de
 * `RingingSoundResolver`, qui journaliserait à tort `RINGTONE_FALLBACK`. Aucune autre table touchée.
 */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE alarm_session ADD COLUMN volumeRampSeconds INTEGER")
            db.execSQL(
                "UPDATE alarm_session SET ringtoneKey = '${LegacyRingtone.REPLACEMENT_KEY}' " +
                    "WHERE ringtoneKey = '${LegacyRingtone.KEY}'",
            )
        }
    }
