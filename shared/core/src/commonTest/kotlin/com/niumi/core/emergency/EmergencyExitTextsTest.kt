package com.niumi.core.emergency

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * `SPECIFICATION.md` §13 : un seul texte validé, recopié mot pour mot avec l'apostrophe
 * typographique (D13). Il compte 196 caractères : le « 197 » de la spec et des maquettes était
 * faux (décision du 2026-10-24, `ETAPE-42.md`).
 */
class EmergencyExitTextsTest {
    private val validatedText =
        "J’avais décidé de bloquer mes applications jusqu’au scan de mon Niumi Spot. " +
            "Je choisis de sortir de cette session sans le scanner. " +
            "Je recopie ce texte en entier pour confirmer que c’est mon choix."

    @Test
    fun onlyTheValidatedTextIsDelivered() {
        assertEquals(listOf("v1-01"), EmergencyExitTexts.ids)
    }

    @Test
    fun theValidatedTextIsCopiedWordForWord() {
        assertEquals(validatedText, EmergencyExitTexts.text("v1-01"))
    }

    @Test
    fun theValidatedTextCountsOneHundredNinetySixCharacters() {
        assertEquals(196, EmergencyExitTexts.charCount("v1-01"))
    }

    @Test
    fun everyTextServesEveningAndMorningAlike() {
        // §13 : le même texte sert le soir et le matin, donc ni « ce soir » ni une heure.
        for (id in EmergencyExitTexts.ids) {
            val text = requireNotNull(EmergencyExitTexts.text(id))
            assertFalse(text.contains("ce soir", ignoreCase = true), id)
            assertFalse(text.any { it.isDigit() }, id)
        }
    }

    @Test
    fun anUnknownIdHasNeitherTextNorCount() {
        assertNull(EmergencyExitTexts.text("v9-99"))
        assertNull(EmergencyExitTexts.charCount("v9-99"))
    }
}
