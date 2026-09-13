package com.niumi.database

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

private const val CURRENT_VERSION = 2

/**
 * Garde-fou contre un `exportSchema` désactivé ou un schéma non committé (SPEC_CORE_KMP §13 :
 * « migration testée avant toute montée de version »). Ce test JVM tourne sans appareil ;
 * `NiumiDatabaseSchemaTest` (instrumenté) exerce la migration elle-même.
 *
 * Le schéma v1 doit rester committé après la montée en v2 : `MigrationTestHelper` en a besoin pour
 * créer une base v1 avant d'y appliquer `MIGRATION_1_2`.
 */
class ExportedSchemaTest {
    private fun schemaFile(version: Int): File {
        val rootDir =
            requireNotNull(System.getProperty("niumi.rootDir")) {
                "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
            }
        return File(rootDir, "androidApp/core/database/schemas/com.niumi.database.NiumiDatabase/$version.json")
    }

    private fun database(version: Int) =
        Json
            .parseToJsonElement(schemaFile(version).readText())
            .jsonObject
            .getValue("database")
            .jsonObject

    @Test
    fun everyVersionUpToTheCurrentOneIsCommitted() {
        (1..CURRENT_VERSION).forEach { version ->
            assertThat(schemaFile(version).exists()).isTrue()
            assertThat(database(version).getValue("version").jsonPrimitive.int).isEqualTo(version)
            assertThat(database(version).getValue("identityHash").jsonPrimitive.content).isNotEmpty()
        }
    }

    @Test
    fun currentSchemaDeclaresTheExpectedEntities() {
        val tableNames =
            database(CURRENT_VERSION).getValue("entities").jsonArray.map {
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

    /** SPEC_ANDROID §17 : chaque événement porte le contexte d'appareil (ajouté en v2, étape 16). */
    @Test
    fun technicalEventCarriesTheContextColumnsSinceVersionTwo() {
        val technicalEvent =
            database(CURRENT_VERSION)
                .getValue("entities")
                .jsonArray
                .map { it.jsonObject }
                .single { it.getValue("tableName").jsonPrimitive.content == "technical_event" }

        val fields =
            technicalEvent.getValue("fields").jsonArray.map {
                it.jsonObject
                    .getValue("fieldPath")
                    .jsonPrimitive.content
            }

        assertThat(fields).containsAtLeast("deviceModel", "androidVersion", "appVersion")
    }
}
