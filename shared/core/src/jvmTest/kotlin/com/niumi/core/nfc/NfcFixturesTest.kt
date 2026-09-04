package com.niumi.core.nfc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Rejoue `fixtures/nfc_payloads.json` (SPEC_CORE_KMP §17 « des fixtures communes décrivent les
 * entrées et résultats attendus »). Ce test n'existe que côté JVM pour l'étape 2 : il valide le
 * fichier de fixtures lui-même avant qu'un test natif Android équivalent ne le consomme.
 */
class NfcFixturesTest {
    @Serializable
    private data class Fixture(
        val uri: String,
        val expected: String,
    )

    @Test
    fun everyFixtureMatchesItsExpectedStatus() {
        val json =
            requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/nfc_payloads.json")) {
                "fixtures/nfc_payloads.json introuvable sur le classpath de test"
            }.bufferedReader().readText()

        val fixtures = Json.decodeFromString<List<Fixture>>(json)

        for (fixture in fixtures) {
            val actual = BoxPayloadParser.parse(fixture.uri).status.name
            assertEquals(fixture.expected, actual, "URI en échec : ${fixture.uri}")
        }
    }
}
