package com.niumi.database

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

/**
 * Garde-fou contre un `exportSchema` désactivé ou un schéma v1 non committé (SPEC_CORE_KMP §13 :
 * « migration testée avant toute montée de version »). En v1, `MigrationTestHelper` n'a aucune
 * migration à exercer (`createDatabase(1)` puis `runMigrationsAndValidate(1, emptyList())` ne
 * ferait que revalider le schéma contre lui-même, ce que Room refait de toute façon à chaque
 * ouverture réelle via son *identity hash*) : ce test JVM, qui tourne sans appareil, apporte
 * davantage de valeur.
 */
class ExportedSchemaTest {
    @Test
    fun exportedSchemaExistsWithTheExpectedVersionAndEntities() {
        val rootDir =
            requireNotNull(System.getProperty("niumi.rootDir")) {
                "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
            }
        val schemaFile =
            File(rootDir, "androidApp/core/database/schemas/com.niumi.database.NiumiDatabase/1.json")

        assertThat(schemaFile.exists()).isTrue()

        val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject
        val database = schema.getValue("database").jsonObject

        assertThat(database.getValue("version").jsonPrimitive.int).isEqualTo(1)
        assertThat(database.getValue("identityHash").jsonPrimitive.content).isNotEmpty()

        val tableNames =
            database.getValue("entities").jsonArray.map {
                it.jsonObject
                    .getValue("tableName")
                    .jsonPrimitive.content
            }
        assertThat(tableNames).containsExactly(
            "alarm_session",
            "blocked_app",
            "paired_box",
            "technical_event",
            "session_incident",
            "session_event_receipt",
            "session_effect_outbox",
            "active_session_pointer",
        )
    }
}
