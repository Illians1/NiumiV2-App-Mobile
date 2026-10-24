package com.niumi.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.database.migration.MIGRATION_1_2
import com.niumi.database.migration.MIGRATION_2_3
import com.niumi.database.migration.MIGRATION_3_4
import com.niumi.database.migration.MIGRATION_4_5
import com.niumi.database.migration.MIGRATION_5_6
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DATABASE_NAME = "niumi-migration-test.db"

/**
 * Prouve que les schémas exportés sont lisibles par `MigrationTestHelper` depuis les assets du
 * module de test, que `MIGRATION_1_2` (étape 16) amène une base v1 peuplée en v2 sans perdre
 * son journal, et que `MIGRATION_2_3` (Lot 6) ajoute les quatre colonnes du début de blocage à
 * une base v2 portant une session `ARMED`, et que `MIGRATION_3_4` (Lot 7) réécrit la sonnerie retirée
 * de l'APK et ajoute la colonne de montée, et que `MIGRATION_5_6` (étape 43) supprime `session_box`
 * sans rien perdre d'autre. `ExportedSchemaTest` (JVM, sans appareil) couvre le
 * reste. Voir `ETAPE-09.md`.
 *
 * L'API `SupportSQLiteDatabase` est utilisée plutôt que celle rendant un `SQLiteConnection` : le
 * constructeur `(Instrumentation, Class)` configure un `SupportSQLiteOpenHelper` et non un
 * `SQLiteDriver`, ce qui rend la seconde indisponible (`IllegalStateException` à l'exécution).
 */
@RunWith(AndroidJUnit4::class)
class NiumiDatabaseSchemaTest {
    @get:Rule
    val migrationTestHelper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), NiumiDatabase::class.java)

    /**
     * Le schéma v1 doit rester committé et lisible après la montée en v2 : `MigrationTestHelper`
     * en a besoin pour créer une base v1 avant d'y appliquer `MIGRATION_1_2`. Le second paramètre
     * de `runMigrationsAndValidate` est la version **cible** — `1` signifie « aucune migration à
     * appliquer », et la base doit alors se valider contre le schéma v1.
     */
    @Test
    fun schemaVersionOneIsReadableAndSelfValidates() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 1).close()

        val validated = migrationTestHelper.runMigrationsAndValidate(TEST_DATABASE_NAME, 1, true)

        assertThat(validated.version).isEqualTo(1)
        validated.close()
    }

    /**
     * Une ligne écrite en v1 survit à la migration et reçoit `''` sur les trois champs de contexte
     * de §17 : le journal antérieur est conservé, pas vidé.
     */
    @Test
    fun migrationOneToTwoAddsTheContextColumnsAndKeepsExistingEvents() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 1).use { v1 ->
            v1.execSQL(
                "INSERT INTO technical_event (sessionId, type, createdAtEpochMillis, detailsJson) " +
                    "VALUES ('session-1', 'ALARM_RECEIVED', 1700000000000, NULL)",
            )
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(TEST_DATABASE_NAME, 2, true, MIGRATION_1_2)

        val cursor =
            migrated.query("SELECT sessionId, type, deviceModel, androidVersion, appVersion FROM technical_event")
        cursor.use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("session-1")
            assertThat(cursor.getString(1)).isEqualTo("ALARM_RECEIVED")
            assertThat(cursor.getString(2)).isEmpty()
            assertThat(cursor.getString(3)).isEmpty()
            assertThat(cursor.getString(4)).isEmpty()
            assertThat(cursor.count).isEqualTo(1)
        }
        migrated.close()
    }

    /**
     * SPEC_ANDROID §7.2 (v3) : la migration est additive et une session `ARMED` en cours pendant la
     * mise à jour reste bloquante. Ses trois premières colonnes restent nulles (blocage immédiat) et
     * `blockingAppliedAtEpochMillis` reçoit `createdAtEpochMillis` — la laisser nulle la ferait relire
     * comme « blocage en attente » et **lèverait le blocage** d'une session active (§12.2). Le journal
     * technique, dans une autre table, doit être conservé intact.
     */
    @Test
    fun migrationTwoToThreeAddsTheBlockingColumnsAndKeepsTheSessionBlocking() {
        val createdAt = 1_700_000_000_000L
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 2).use { v2 ->
            v2.execSQL(
                "INSERT INTO alarm_session (id, schemaVersion, revision, localDate, localTime, " +
                    "zoneIdAtActivation, triggerAtEpochMillis, state, releaseTarget, health, boxId, " +
                    "boxTokenSha256Hex, ringtoneKey, vibrationEnabled, createdAtEpochMillis, " +
                    "armedAtEpochMillis, ringingAtEpochMillis, alarmSoundStoppedAtEpochMillis, " +
                    "triggerElapsedAtEpochMillis, nfcVerifiedAtEpochMillis, releasingAtEpochMillis, " +
                    "completedAtEpochMillis, cancelledAtEpochMillis, failureCode) " +
                    "VALUES ('session-1', 1, 2, '2026-09-08', '07:00', 'Europe/Paris', 1800000000000, " +
                    "'ARMED', NULL, 'HEALTHY', 'box-1', '" + "a".repeat(64) + "', 'niumi_default', 1, " +
                    "$createdAt, 1700000001000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)",
            )
            v2.execSQL(
                "INSERT INTO technical_event (sessionId, type, createdAtEpochMillis, detailsJson, " +
                    "deviceModel, androidVersion, appVersion) " +
                    "VALUES ('session-1', 'SESSION_ARMED', 1700000001000, NULL, 'Pixel', '16', '1.0')",
            )
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(TEST_DATABASE_NAME, 3, true, MIGRATION_2_3)

        migrated
            .query(
                "SELECT schemaVersion, blockingLocalDate, blockingLocalTime, " +
                    "blockingStartsAtEpochMillis, blockingAppliedAtEpochMillis FROM alarm_session",
            ).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(2)
                assertThat(cursor.isNull(1)).isTrue()
                assertThat(cursor.isNull(2)).isTrue()
                assertThat(cursor.isNull(3)).isTrue()
                assertThat(cursor.getLong(4)).isEqualTo(createdAt)
                assertThat(cursor.count).isEqualTo(1)
            }
        migrated.query("SELECT type, deviceModel FROM technical_event").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("SESSION_ARMED")
            assertThat(cursor.getString(1)).isEqualTo("Pixel")
            assertThat(cursor.count).isEqualTo(1)
        }
        migrated.close()
    }

    /**
     * SPEC_ANDROID §7.2 (v4, Lot 7) : une session `ARMED` sur la sonnerie du MVP — retirée de l'APK —
     * passe sur `niumi_piano` à volume constant (`volumeRampSeconds` nul), sans quoi elle sonnerait
     * par le repli et journaliserait à tort `RINGTONE_FALLBACK`. Une autre clé n'est pas touchée,
     * ni aucune autre colonne ou table.
     */
    @Test
    fun migrationThreeToFourRewritesTheLegacyRingtoneAndAddsTheRampColumn() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 3).use { v3 ->
            listOf("session-1" to "niumi_alarm", "session-2" to "autre_sonnerie").forEach { (id, ringtone) ->
                v3.execSQL(
                    "INSERT INTO alarm_session (id, schemaVersion, revision, localDate, localTime, " +
                        "zoneIdAtActivation, triggerAtEpochMillis, blockingLocalDate, blockingLocalTime, " +
                        "blockingStartsAtEpochMillis, blockingAppliedAtEpochMillis, state, releaseTarget, " +
                        "health, boxId, boxTokenSha256Hex, ringtoneKey, vibrationEnabled, createdAtEpochMillis, " +
                        "armedAtEpochMillis, ringingAtEpochMillis, alarmSoundStoppedAtEpochMillis, " +
                        "triggerElapsedAtEpochMillis, nfcVerifiedAtEpochMillis, releasingAtEpochMillis, " +
                        "completedAtEpochMillis, cancelledAtEpochMillis, failureCode) " +
                        "VALUES ('$id', 2, 2, '2026-09-30', '07:00', 'Europe/Paris', 1800000000000, " +
                        "'2026-09-29', '22:30', 1790000000000, NULL, 'ARMED', NULL, 'HEALTHY', 'box-1', '" +
                        "a".repeat(64) + "', '$ringtone', 1, 1700000000000, 1700000001000, NULL, NULL, NULL, " +
                        "NULL, NULL, NULL, NULL, NULL)",
                )
            }
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(TEST_DATABASE_NAME, 4, true, MIGRATION_3_4)

        migrated
            .query(
                "SELECT id, ringtoneKey, volumeRampSeconds, blockingStartsAtEpochMillis, " +
                    "blockingAppliedAtEpochMillis, revision FROM alarm_session ORDER BY id",
            ).use { cursor ->
                assertThat(cursor.count).isEqualTo(2)
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(1)).isEqualTo("niumi_piano")
                assertThat(cursor.isNull(2)).isTrue()
                assertThat(cursor.getLong(3)).isEqualTo(1_790_000_000_000L)
                assertThat(cursor.isNull(4)).isTrue()
                assertThat(cursor.getLong(5)).isEqualTo(2)
                assertThat(cursor.moveToNext()).isTrue()
                assertThat(cursor.getString(1)).isEqualTo("autre_sonnerie")
                assertThat(cursor.isNull(2)).isTrue()
            }
        migrated.close()
    }

    /**
     * SPEC_ANDROID §7.2 (v5, Lot 8). Point de vigilance 1 du lot : `paired_box` porte le boîtier
     * B (remplacé hors session, permis jusqu'ici), mais la session `ARMED` a figé le boîtier A.
     * `session_box` doit se remplir depuis `alarm_session` (A), **jamais** depuis `paired_box`
     * (qui contiendrait B). Un reçu, une ligne d'outbox, une application bloquée, un incident et le
     * pointeur de session active prouvent qu'aucune donnée n'est perdue par un `CASCADE` qui n'a
     * pas lieu.
     */
    @Test
    fun migrationFourToFiveMovesTheFrozenBoxIntoSessionBoxAndDropsTheColumns() {
        val boxA = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        val boxB = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 4).use { v4 -> seedV4ArmedSession(v4, boxA, boxB) }

        val migrated = migrationTestHelper.runMigrationsAndValidate(TEST_DATABASE_NAME, 5, true, MIGRATION_4_5)

        migrated.query("SELECT sessionId, boxId, tokenSha256, name, position FROM session_box").use { cursor ->
            assertThat(cursor.count).isEqualTo(1)
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("session-1")
            assertThat(cursor.getString(1)).isEqualTo(boxA)
            assertThat(cursor.getString(2)).isEqualTo("a".repeat(64))
            assertThat(cursor.getString(3)).isEqualTo("Boîtier 1")
            assertThat(cursor.getInt(4)).isEqualTo(0)
        }
        migrated.query("PRAGMA table_info(alarm_session)").use { cursor ->
            val columns = generateSequence { if (cursor.moveToNext()) cursor.getString(1) else null }.toList()
            assertThat(columns).doesNotContain("boxId")
            assertThat(columns).doesNotContain("boxTokenSha256Hex")
        }
        migrated.query("SELECT id, revision, state FROM alarm_session").use { cursor ->
            assertThat(cursor.count).isEqualTo(1)
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("session-1")
            assertThat(cursor.getLong(1)).isEqualTo(3)
            assertThat(cursor.getString(2)).isEqualTo("ARMED")
        }
        migrated.query("SELECT boxId, name, position FROM paired_box").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo(boxB)
            assertThat(cursor.getString(1)).isEqualTo("Boîtier 1")
            assertThat(cursor.getInt(2)).isEqualTo(0)
        }
        assertEachChildTableKeptExactlyOneRowForSessionOne(migrated)
        migrated.close()
    }

    /**
     * SPEC_ANDROID §7.2 (v6, étape 43). Une base v5 porte une session `ARMED` avec deux boîtiers
     * figés dans `session_box` et les deux mêmes boîtiers dans `paired_box`, plus une session
     * `COMPLETED` libérée par scan. Après migration : `session_box` a disparu, les deux boîtiers et
     * leurs noms restent dans `paired_box` (seule source de la vérification désormais), les cinq
     * tables enfants restantes gardent leurs lignes, `releasedBy` vaut `NFC_SCAN` pour la session
     * scannée et reste nul pour la session armée, et toutes les sessions passent au schéma 3.
     */
    @Test
    fun migrationFiveToSixKeepsTheArmedSessionDropsSessionBoxAndDeducesReleasedBy() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 5).use { v5 -> seedV5Sessions(v5) }

        val migrated = migrationTestHelper.runMigrationsAndValidate(TEST_DATABASE_NAME, 6, true, MIGRATION_5_6)

        migrated.query("SELECT name FROM sqlite_master WHERE name LIKE '%session_box%'").use { cursor ->
            assertThat(cursor.count).isEqualTo(0)
        }
        migrated
            .query(
                "SELECT id, state, revision, schemaVersion, releasedBy, disarmedAtEpochMillis " +
                    "FROM alarm_session ORDER BY id",
            ).use { cursor ->
                assertThat(cursor.count).isEqualTo(2)
                cursor.moveToFirst()
                assertThat(cursor.getString(0)).isEqualTo("session-1")
                assertThat(cursor.getString(1)).isEqualTo("ARMED")
                assertThat(cursor.getLong(2)).isEqualTo(3)
                assertThat(cursor.getInt(3)).isEqualTo(3)
                assertThat(cursor.isNull(4)).isTrue()
                assertThat(cursor.isNull(5)).isTrue()
                cursor.moveToNext()
                assertThat(cursor.getString(0)).isEqualTo("session-2")
                assertThat(cursor.getString(1)).isEqualTo("COMPLETED")
                assertThat(cursor.getInt(3)).isEqualTo(3)
                assertThat(cursor.getString(4)).isEqualTo("NFC_SCAN")
                assertThat(cursor.isNull(5)).isTrue()
            }
        migrated.query("SELECT boxId, name, position FROM paired_box ORDER BY position").use { cursor ->
            val boxes =
                generateSequence {
                    if (cursor.moveToNext()) {
                        cursor.getString(
                            1,
                        ) to cursor.getInt(2)
                    } else {
                        null
                    }
                }.toList()
            assertThat(boxes).containsExactly("Cuisine" to 0, "Salon" to 1).inOrder()
        }
        assertEachChildTableKeptExactlyOneRowForSessionOne(migrated)
        migrated.close()
    }

    private fun seedV5Sessions(v5: SupportSQLiteDatabase) {
        listOf(
            "'session-1', 3, 'ARMED', NULL, NULL",
            "'session-2', 9, 'COMPLETED', 1700000005000, 1700000006000",
        ).forEach { values ->
            v5.execSQL(
                "INSERT INTO alarm_session (id, revision, state, nfcVerifiedAtEpochMillis, completedAtEpochMillis, " +
                    "schemaVersion, localDate, localTime, zoneIdAtActivation, triggerAtEpochMillis, health, " +
                    "ringtoneKey, vibrationEnabled, createdAtEpochMillis) VALUES ($values, 2, '2026-10-16', " +
                    "'07:00', 'Europe/Paris', 1800000000000, 'HEALTHY', 'niumi_piano', 1, 1700000000000)",
            )
        }
        listOf("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" to "Cuisine", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" to "Salon")
            .forEachIndexed { position, (boxId, name) ->
                val token = boxId.first().toString().repeat(64)
                v5.execSQL(
                    "INSERT INTO paired_box (boxId, protocolVersion, tokenSha256, pairedAtEpochMillis, name, " +
                        "position) VALUES ('$boxId', 1, '$token', 1700000002000, '$name', $position)",
                )
                v5.execSQL(
                    "INSERT INTO session_box (sessionId, boxId, tokenSha256, name, position) " +
                        "VALUES ('session-1', '$boxId', '$token', '$name', $position)",
                )
            }
        seedChildRowsForSessionOne(v5)
    }

    /**
     * Point de vigilance 1 du Lot 8 : `paired_box` porte le boîtier B (remplacé hors session,
     * permis jusqu'ici), mais la session `ARMED` a figé le boîtier A. Un reçu, une ligne
     * d'outbox, une application bloquée, un incident et le pointeur actif prouvent qu'aucune
     * donnée n'est perdue.
     */
    private fun seedV4ArmedSession(
        v4: SupportSQLiteDatabase,
        boxA: String,
        boxB: String,
    ) {
        v4.execSQL(
            "INSERT INTO alarm_session (id, schemaVersion, revision, localDate, localTime, " +
                "zoneIdAtActivation, triggerAtEpochMillis, blockingLocalDate, blockingLocalTime, " +
                "blockingStartsAtEpochMillis, blockingAppliedAtEpochMillis, state, releaseTarget, " +
                "health, boxId, boxTokenSha256Hex, ringtoneKey, vibrationEnabled, volumeRampSeconds, " +
                "createdAtEpochMillis, armedAtEpochMillis, ringingAtEpochMillis, " +
                "alarmSoundStoppedAtEpochMillis, triggerElapsedAtEpochMillis, nfcVerifiedAtEpochMillis, " +
                "releasingAtEpochMillis, completedAtEpochMillis, cancelledAtEpochMillis, failureCode) " +
                "VALUES ('session-1', 2, 3, '2026-10-16', '07:00', 'Europe/Paris', 1800000000000, " +
                "NULL, NULL, NULL, 1700000000000, 'ARMED', NULL, 'HEALTHY', '$boxA', '" +
                "a".repeat(64) + "', 'niumi_default', 1, NULL, 1700000000000, 1700000001000, " +
                "NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)",
        )
        v4.execSQL(
            "INSERT INTO paired_box (boxId, protocolVersion, tokenSha256, pairedAtEpochMillis) " +
                "VALUES ('$boxB', 1, '" + "b".repeat(64) + "', 1700000002000)",
        )
        seedChildRowsForSessionOne(v4)
    }

    /**
     * Un reçu, une ligne d'outbox, une application bloquée, un incident et le pointeur actif : les
     * cinq tables enfants dont [assertEachChildTableKeptExactlyOneRowForSessionOne] vérifie qu'aucune
     * migration ne les vide. Colonnes identiques de la v4 à la v6.
     */
    private fun seedChildRowsForSessionOne(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO session_event_receipt (eventId, sessionId, payloadSha256Hex, " +
                "appliedRevision, receivedAtEpochMillis) VALUES ('event-1', 'session-1', '" +
                "c".repeat(64) + "', 3, 1700000003000)",
        )
        db.execSQL(
            "INSERT INTO session_effect_outbox (effectId, sessionId, revision, kind, ordinal, " +
                "payloadJson, status, lastError, updatedAtEpochMillis) VALUES " +
                "('session-1:3:SCHEDULE_ALARM:0', 'session-1', 3, 'SCHEDULE_ALARM', 0, NULL, " +
                "'SUCCEEDED', NULL, 1700000003000)",
        )
        db.execSQL(
            "INSERT INTO blocked_app (sessionId, packageName, displayNameSnapshot) " +
                "VALUES ('session-1', 'com.example.first', 'Première application')",
        )
        db.execSQL(
            "INSERT INTO session_incident (sessionId, code, severity, occurredAtEpochMillis, platform) " +
                "VALUES ('session-1', 'NFC_DISABLED', 'CRITICAL', 1700000004000, 'ANDROID')",
        )
        db.execSQL("INSERT INTO active_session_pointer (id, sessionId) VALUES (1, 'session-1')")
    }

    /**
     * Aucune des cinq tables enfants de la v4 n'a été vidée par le `CASCADE` du `DROP TABLE
     * alarm_session` — le pointeur de session active compris, dont la perte afficherait « Aucune
     * session » alors que l'alarme reste programmée.
     */
    private fun assertEachChildTableKeptExactlyOneRowForSessionOne(migrated: SupportSQLiteDatabase) {
        listOf(
            "session_event_receipt",
            "session_effect_outbox",
            "blocked_app",
            "session_incident",
            "active_session_pointer",
        ).forEach { table ->
            migrated.query("SELECT COUNT(*) FROM $table WHERE sessionId = 'session-1'").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(1)
            }
        }
    }
}
