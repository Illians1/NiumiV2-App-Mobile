package com.niumi.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.database.migration.MIGRATION_1_2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DATABASE_NAME = "niumi-migration-test.db"

/**
 * Prouve que les schémas exportés sont lisibles par `MigrationTestHelper` depuis les assets du
 * module de test, et que `MIGRATION_1_2` (étape 16) amène une base v1 peuplée en v2 sans perdre
 * son journal. `ExportedSchemaTest` (JVM, sans appareil) couvre le reste. Voir `ETAPE-09.md`.
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
}
