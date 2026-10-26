package com.niumi.feature.session.success

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

/**
 * Écran 11 · Réussite (`SPECIFICATION.md` §15, maquette 42 ; étape 46), mot pour mot, tutoiement,
 * apostrophe typographique (D13). Délais accordés, puis en heures dès 60 min (décision de Mehdi du
 * 2026-10-25).
 */
class SuccessTextsTest {
    @Test
    fun theTitleGivesTheTimeOfTheScanWithoutLeadingZero() {
        assertThat(SuccessTexts.standingAt(LocalTime.of(7, 4))).isEqualTo("Debout à 7:04.")
        assertThat(SuccessTexts.standingAt(LocalTime.of(10, 30))).isEqualTo("Debout à 10:30.")
    }

    @Test
    fun theDelayIsWrittenAsInTheMockup() {
        assertThat(SuccessTexts.afterAlarm(4)).isEqualTo("4 minutes après la sonnerie.")
        assertThat(SuccessTexts.afterAlarm(59)).isEqualTo("59 minutes après la sonnerie.")
    }

    @Test
    fun aDelayUnderTwoMinutesIsAgreed() {
        assertThat(SuccessTexts.afterAlarm(0)).isEqualTo("Moins d’une minute après la sonnerie.")
        assertThat(SuccessTexts.afterAlarm(1)).isEqualTo("1 minute après la sonnerie.")
    }

    @Test
    fun aDelayOfAnHourOrMoreUsesTheSharedDurationFormat() {
        assertThat(SuccessTexts.afterAlarm(60)).isEqualTo("1 h 00 après la sonnerie.")
        assertThat(SuccessTexts.afterAlarm(75)).isEqualTo("1 h 15 après la sonnerie.")
    }

    @Test
    fun theComparisonStartsWithBravoInBold() {
        assertThat(SuccessTexts.BRAVO).isEqualTo("Bravo !")
        assertThat(SuccessTexts.savedComparison(11)).isEqualTo("C’est 11 minutes de moins qu’avant Niumi.")
        assertThat(SuccessTexts.savedComparison(1)).isEqualTo("C’est 1 minute de moins qu’avant Niumi.")
        assertThat(SuccessTexts.savedComparison(60)).isEqualTo("C’est 1 h 00 de moins qu’avant Niumi.")
    }

    @Test
    fun theOtherTextsAreQuotedWordForWord() {
        assertThat(SuccessTexts.APPS_BACK).isEqualTo("Tes applications sont de nouveau accessibles.")
        assertThat(SuccessTexts.BACK_HOME).isEqualTo("Retour à l’accueil")
    }

    @Test
    fun noTextCarriesAStraightApostropheOrTheFormalAddress() {
        val all =
            listOf(
                SuccessTexts.standingAt(LocalTime.of(7, 4)),
                SuccessTexts.afterAlarm(0),
                SuccessTexts.afterAlarm(4),
                SuccessTexts.BRAVO,
                SuccessTexts.savedComparison(11),
                SuccessTexts.APPS_BACK,
                SuccessTexts.BACK_HOME,
            )
        assertThat(all.filter { it.contains('\'') }).isEmpty()
        assertThat(all.filter { Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""").containsMatchIn(it) }).isEmpty()
    }
}
