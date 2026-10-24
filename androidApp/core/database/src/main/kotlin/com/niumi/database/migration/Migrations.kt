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

/**
 * v4 → v5 (Lot 8, boîtiers multiples) : `paired_box` gagne `name` et `position` ; une nouvelle
 * table `session_box` fige la liste des boîtiers d'une session, sur le modèle exact de
 * `blocked_app` ; `alarm_session` perd `boxId` et `boxTokenSha256Hex`, copiés dans `session_box`.
 *
 * `session_box` est peuplée **depuis `alarm_session`, jamais depuis `paired_box`** (point de
 * vigilance 1 du Lot 8) : une session armée puis un boîtier remplacé hors session (permis
 * jusqu'ici) laisse les deux tables en désaccord, et seule la paire figée dans la session doit se
 * retrouver dans `session_box`.
 *
 * `alarm_session` est recréée en suivant la procédure officielle SQLite de recréation de table
 * (`sqlite.org/lang_altertable.html`, § « Making Other Kinds Of Table Schema Changes ») : SQLite
 * ne retire pas une colonne d'une table peuplée autrement. `PRAGMA foreign_keys` n'a volontairement
 * pas sa place ici : la pragma ne peut être changée à l'intérieur d'une transaction (elle est sans
 * effet dans une migration Room, qui en exécute toujours une), et Room ne la met à `ON` que dans
 * `onOpen()`, **après** les migrations — c'est cela, et non une pragma, qui empêche le `DROP TABLE
 * alarm_session` de déclencher le `CASCADE` des tables enfants. `PRAGMA foreign_key_check` ferme la
 * migration : toute violation fait échouer l'ouverture plutôt que de livrer une base incohérente.
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE paired_box ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE paired_box SET name = 'Boîtier 1'")
            db.execSQL("ALTER TABLE paired_box ADD COLUMN position INTEGER NOT NULL DEFAULT 0")

            db.execSQL(
                "CREATE TABLE session_box (" +
                    "sessionId TEXT NOT NULL, boxId TEXT NOT NULL, tokenSha256 TEXT NOT NULL, " +
                    "name TEXT NOT NULL, position INTEGER NOT NULL, " +
                    "PRIMARY KEY(sessionId, boxId), " +
                    "FOREIGN KEY(sessionId) REFERENCES alarm_session(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX index_session_box_sessionId ON session_box(sessionId)")
            db.execSQL(
                "INSERT INTO session_box SELECT id, boxId, boxTokenSha256Hex, 'Boîtier 1', 0 FROM alarm_session",
            )

            db.execSQL(
                "CREATE TABLE alarm_session_new (" +
                    "id TEXT NOT NULL, schemaVersion INTEGER NOT NULL, revision INTEGER NOT NULL, " +
                    "localDate TEXT NOT NULL, localTime TEXT NOT NULL, zoneIdAtActivation TEXT NOT NULL, " +
                    "triggerAtEpochMillis INTEGER NOT NULL, blockingLocalDate TEXT, blockingLocalTime TEXT, " +
                    "blockingStartsAtEpochMillis INTEGER, blockingAppliedAtEpochMillis INTEGER, " +
                    "state TEXT NOT NULL, releaseTarget TEXT, health TEXT NOT NULL, " +
                    "ringtoneKey TEXT NOT NULL, vibrationEnabled INTEGER NOT NULL, volumeRampSeconds INTEGER, " +
                    "createdAtEpochMillis INTEGER NOT NULL, armedAtEpochMillis INTEGER, " +
                    "ringingAtEpochMillis INTEGER, alarmSoundStoppedAtEpochMillis INTEGER, " +
                    "triggerElapsedAtEpochMillis INTEGER, nfcVerifiedAtEpochMillis INTEGER, " +
                    "releasingAtEpochMillis INTEGER, completedAtEpochMillis INTEGER, " +
                    "cancelledAtEpochMillis INTEGER, failureCode TEXT, PRIMARY KEY(id))",
            )
            db.execSQL(
                "INSERT INTO alarm_session_new SELECT " +
                    "id, schemaVersion, revision, localDate, localTime, zoneIdAtActivation, " +
                    "triggerAtEpochMillis, blockingLocalDate, blockingLocalTime, blockingStartsAtEpochMillis, " +
                    "blockingAppliedAtEpochMillis, state, releaseTarget, health, " +
                    "ringtoneKey, vibrationEnabled, volumeRampSeconds, createdAtEpochMillis, " +
                    "armedAtEpochMillis, ringingAtEpochMillis, alarmSoundStoppedAtEpochMillis, " +
                    "triggerElapsedAtEpochMillis, nfcVerifiedAtEpochMillis, releasingAtEpochMillis, " +
                    "completedAtEpochMillis, cancelledAtEpochMillis, failureCode " +
                    "FROM alarm_session",
            )
            db.execSQL("DROP TABLE alarm_session")
            db.execSQL("ALTER TABLE alarm_session_new RENAME TO alarm_session")

            db.query("PRAGMA foreign_key_check").use { cursor ->
                check(cursor.count == 0) { "MIGRATION_4_5: foreign_key_check a trouvé une violation." }
            }
        }
    }

/**
 * v5 → v6 (étape 43, contrat 1.5) : `alarm_session` gagne `releasedBy` et `disarmedAtEpochMillis`,
 * et `session_box` disparaît — la vérification d'un scan lit l'ensemble courant de `paired_box`
 * (SPEC_CORE_KMP §2, décisions 11 et 16 ; décision Q11 du 2026-10-19).
 *
 * Les deux colonnes sont nullables et ajoutées **sans clause `DEFAULT`**, comme celles de
 * [MIGRATION_2_3] : Room n'en attend aucune, et `validateMigration` échouerait sur cette seule
 * différence. `releasedBy` est déduit pour les sessions existantes selon la règle de lecture d'un
 * snapshot v2 (SPEC_CORE_KMP §7.1) : `NFC_SCAN` si `nfcVerifiedAtEpochMillis` est renseigné, nul
 * sinon — avant le contrat 1.5, le scan était le seul moyen de libération. Toutes les sessions
 * passent ainsi au schéma de domaine 3.
 *
 * `alarm_session` n'est pas recréée : seul `DROP TABLE session_box` retire une table, enfant et non
 * parente, et aucun `CASCADE` n'entre en jeu. L'index `index_session_box_sessionId` part avec elle.
 */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE alarm_session ADD COLUMN releasedBy TEXT")
            db.execSQL("ALTER TABLE alarm_session ADD COLUMN disarmedAtEpochMillis INTEGER")
            db.execSQL("UPDATE alarm_session SET releasedBy = 'NFC_SCAN' WHERE nfcVerifiedAtEpochMillis IS NOT NULL")
            db.execSQL("UPDATE alarm_session SET schemaVersion = 3")
            db.execSQL("DROP TABLE session_box")
        }
    }
