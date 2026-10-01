package com.niumi.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.database.migration.MIGRATION_1_2
import com.niumi.database.migration.MIGRATION_2_3
import com.niumi.database.migration.MIGRATION_3_4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DATABASE_NAME = "niumi-migration-test.db"

/**
 * Prouve que les schémas exportés sont lisibles par `MigrationTestHelper` depuis les assets du
 * module de test, que `MIGRATION_1_2` (étape 16) amène une base v1 peuplée en v2 sans perdre
 * son journal, et que `MIGRATION_2_3` (Lot 6) ajoute les quatre colonnes du début de blocage à
 * une base v2 portant une session `ARMED`, et que `MIGRATION_3_4` (Lot 7) réécrit la sonnerie retirée
 * de l'APK et ajoute la colonne de montée. `ExportedSchemaTest` (JVM, sans appareil) couvre le
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
}
