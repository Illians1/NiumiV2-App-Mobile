package com.niumi.database

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

private const val CURRENT_VERSION = 6

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

    /** SPEC_ANDROID §7.2 (v3, Lot 6) : les quatre colonnes du début de blocage, toutes nullables. */
    @Test
    fun alarmSessionCarriesTheBlockingColumnsSinceVersionThree() {
        val alarmSession =
            database(CURRENT_VERSION)
                .getValue("entities")
                .jsonArray
                .map { it.jsonObject }
                .single { it.getValue("tableName").jsonPrimitive.content == "alarm_session" }

        val fields =
            alarmSession.getValue("fields").jsonArray.map {
                it.jsonObject
                    .getValue("fieldPath")
                    .jsonPrimitive.content
            }

        assertThat(fields).containsAtLeast(
            "blockingLocalDate",
            "blockingLocalTime",
            "blockingStartsAtEpochMillis",
            "blockingAppliedAtEpochMillis",
        )

        // Toutes nullables : `MIGRATION_2_3` les ajoute sans clause `DEFAULT`, et Room n'en attend
        // donc aucune. Une seule colonne devenue `NOT NULL` ferait échouer `validateMigration` sur
        // cette seule différence — le défaut que `MIGRATION_1_2` avait dû traiter en v2.
        val blockingColumns =
            alarmSession.getValue("fields").jsonArray.map { it.jsonObject }.filter {
                it
                    .getValue("fieldPath")
                    .jsonPrimitive.content
                    .startsWith("blocking")
            }
        assertThat(blockingColumns.none { it["notNull"]?.jsonPrimitive?.content == "true" }).isTrue()
    }

    /** SPEC_ANDROID §7.2 (v4, Lot 7) : `volumeRampSeconds`, nullable — `null` vaut volume constant. */
    @Test
    fun alarmSessionCarriesANullableRampColumnSinceVersionFour() {
        val rampColumn =
            database(CURRENT_VERSION)
                .getValue("entities")
                .jsonArray
                .map { it.jsonObject }
                .single { it.getValue("tableName").jsonPrimitive.content == "alarm_session" }
                .getValue("fields")
                .jsonArray
                .map { it.jsonObject }
                .single { it.getValue("fieldPath").jsonPrimitive.content == "volumeRampSeconds" }

        assertThat(rampColumn.getValue("affinity").jsonPrimitive.content).isEqualTo("INTEGER")
        assertThat(rampColumn["notNull"]?.jsonPrimitive?.content).isNotEqualTo("true")
    }

    /** SPEC_ANDROID §7.2 (v5, Lot 8) : `boxId` et `boxTokenSha256Hex` ne sont plus des colonnes d'`alarm_session`. */
    @Test
    fun alarmSessionNoLongerCarriesTheBoxColumnsSinceVersionFive() {
        val fields =
            database(CURRENT_VERSION)
                .getValue("entities")
                .jsonArray
                .map { it.jsonObject }
                .single { it.getValue("tableName").jsonPrimitive.content == "alarm_session" }
                .getValue("fields")
                .jsonArray
                .map {
                    it.jsonObject
                        .getValue("fieldPath")
                        .jsonPrimitive.content
                }

        assertThat(fields).doesNotContain("boxId")
        assertThat(fields).doesNotContain("boxTokenSha256Hex")
    }

    /**
     * SPEC_ANDROID §7.2 (v6, étape 43) : `session_box` disparaît — la vérification d'un scan lit
     * l'ensemble courant des boîtiers (SPEC_CORE_KMP §2, décisions 11 et 16). Le schéma v5 la garde.
     */
    @Test
    fun sessionBoxIsDroppedSinceVersionSix() {
        fun tableNames(version: Int) =
            database(version).getValue("entities").jsonArray.map {
                it.jsonObject
                    .getValue("tableName")
                    .jsonPrimitive.content
            }

        assertThat(tableNames(5)).contains("session_box")
        assertThat(tableNames(CURRENT_VERSION)).doesNotContain("session_box")
    }

    /**
     * SPEC_ANDROID §7.2 (v6, étape 43) : `releasedBy` et `disarmedAtEpochMillis` (contrat 1.5),
     * nullables comme les colonnes de v3 — `MIGRATION_5_6` les ajoute sans clause `DEFAULT`.
     */
    @Test
    fun alarmSessionCarriesTheNullableReleaseColumnsSinceVersionSix() {
        val fields =
            database(CURRENT_VERSION)
                .getValue("entities")
                .jsonArray
                .map { it.jsonObject }
                .single { it.getValue("tableName").jsonPrimitive.content == "alarm_session" }
                .getValue("fields")
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.getValue("fieldPath").jsonPrimitive.content }

        val releasedBy = fields.getValue("releasedBy")
        val disarmedAt = fields.getValue("disarmedAtEpochMillis")
        assertThat(releasedBy.getValue("affinity").jsonPrimitive.content).isEqualTo("TEXT")
        assertThat(disarmedAt.getValue("affinity").jsonPrimitive.content).isEqualTo("INTEGER")
        assertThat(releasedBy["notNull"]?.jsonPrimitive?.content).isNotEqualTo("true")
        assertThat(disarmedAt["notNull"]?.jsonPrimitive?.content).isNotEqualTo("true")
    }

    /** SPEC_ANDROID §11.1 (v5, Lot 8) : `paired_box` gagne `name` et `position`. */
    @Test
    fun pairedBoxCarriesNameAndPositionSinceVersionFive() {
        val fields =
            database(CURRENT_VERSION)
                .getValue("entities")
                .jsonArray
                .map { it.jsonObject }
                .single { it.getValue("tableName").jsonPrimitive.content == "paired_box" }
                .getValue("fields")
                .jsonArray
                .map {
                    it.jsonObject
                        .getValue("fieldPath")
                        .jsonPrimitive.content
                }

        assertThat(fields).containsAtLeast("name", "position")
    }
}
