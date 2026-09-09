package com.niumi.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DATABASE_NAME = "niumi-migration-test.db"

/**
 * Aucune migration à exercer en v1 : ce test prouve seulement que le schéma exporté est lisible
 * par `MigrationTestHelper` depuis les assets du module de test, et que la base créée à partir de
 * lui se valide. `ExportedSchemaTest` (JVM, sans appareil) couvre l'essentiel. Voir `ETAPE-09.md`.
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

    @Test
    fun schemaVersionOneIsReadableAndSelfValidates() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 1).close()

        val migrated = migrationTestHelper.runMigrationsAndValidate(TEST_DATABASE_NAME, 1, true)

        assertThat(migrated.version).isEqualTo(1)
        migrated.close()
    }
}
