package com.niumi.core.nfc

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotEquals

/**
 * 200 chaînes générées à partir d'une graine fixe, mêlant caractères de contrôle, surrogates
 * isolés et longueurs de 0 à 200 (SPEC_CORE_KMP §17 « NFC », dernier point). Le parseur ne doit
 * jamais lancer d'exception (une exception non attrapée fait échouer ce test directement, sans
 * capture générique) et ne jamais reconnaître ces chaînes comme valides.
 */
class BoxPayloadFuzzTest {
    private val controlChars = (0x00..0x1f).map { it.toChar() } + '\u007f'
    private val highSurrogates = (0xd800..0xdbff step 173).map { it.toChar() }
    private val lowSurrogates = (0xdc00..0xdfff step 173).map { it.toChar() }
    private val printableChars = (0x20..0x7e).map { it.toChar() }
    private val alphabet = controlChars + highSurrogates + lowSurrogates + printableChars

    @Test
    fun neverThrowsAndNeverAcceptsRandomNoise() {
        val random = Random(seed = 42)
        repeat(200) { iteration ->
            val length = random.nextInt(0, 201)
            val chars = CharArray(length) { alphabet[random.nextInt(alphabet.size)] }
            val candidate = chars.concatToString()

            val result = BoxPayloadParser.parse(candidate)

            assertNotEquals(
                BoxPayloadStatus.VALID,
                result.status,
                "itération $iteration a été acceptée à tort : $candidate",
            )
        }
    }
}
