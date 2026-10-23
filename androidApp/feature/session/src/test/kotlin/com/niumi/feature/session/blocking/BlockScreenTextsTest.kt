package com.niumi.feature.session.blocking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Écran 7 · Écran de blocage (étape 39), d'après `design-plan/SPECIFICATION.md` §12 et la
 * maquette 22, avec le nom de l'application dans le titre sur Android (décision de Mehdi du
 * 2026-10-23) : « L’application YouTube est bloquée », accordé avec « application » quel que soit
 * le nom, et une phrase sans pronom genré dessous. Le titre s'écrit sans point final, comme dans le
 * tableau de la spécification.
 */
class BlockScreenTextsTest {
    @Test
    fun theTitleNamesTheBlockedApplication() {
        assertThat(BlockScreenTexts.title("YouTube")).isEqualTo("L’application YouTube est bloquée")
        assertThat(BlockScreenTexts.title("  Instagram ")).isEqualTo("L’application Instagram est bloquée")
    }

    /** Nom inconnu ou vide : le titre de la maquette. */
    @Test
    fun withoutANameTheTitleIsTheOneOfTheMockup() {
        assertThat(BlockScreenTexts.title(null)).isEqualTo("Cette application est bloquée")
        assertThat(BlockScreenTexts.title(" ")).isEqualTo("Cette application est bloquée")
    }

    @Test
    fun theRestOfTheScreenIsQuotedWordForWord() {
        assertThat(BlockScreenTexts.BODY).isEqualTo("Tu pourras l’ouvrir après avoir scanné ton Niumi Point.")
        assertThat(BlockScreenTexts.CLOSE).isEqualTo("Fermer")
        assertThat(BlockScreenTexts.OPEN_NIUMI).isEqualTo("Ouvrir Niumi")
    }

    /** D13 : l'apostrophe droite se glisse par recopie ; elle n'a pas sa place dans l'app. */
    @Test
    fun noTextCarriesAStraightApostrophe() {
        assertThat(allTexts.filter { it.contains('\'') }).isEmpty()
    }

    /** Tutoiement partout (`SPECIFICATION.md` §2). */
    @Test
    fun noTextSwitchesToTheFormalAddress() {
        assertThat(allTexts.filter { FORMAL_ADDRESS.containsMatchIn(it) }).isEmpty()
    }

    private companion object {
        val FORMAL_ADDRESS = Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""")

        val allTexts =
            listOf(
                BlockScreenTexts.title(null),
                BlockScreenTexts.title("YouTube"),
                BlockScreenTexts.BODY,
                BlockScreenTexts.CLOSE,
                BlockScreenTexts.OPEN_NIUMI,
            )
    }
}
