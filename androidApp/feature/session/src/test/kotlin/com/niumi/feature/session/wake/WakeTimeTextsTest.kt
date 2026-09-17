package com.niumi.feature.session.wake

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.feature.session.ui.WakeScheduleFormatter
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** Tutoiement partout (SPEC_ANDROID §15) : aucun texte de cet écran ne dit « vous » ou « votre ». */
class WakeTimeTextsTest {
    private val allTexts =
        listOf(
            WakeTimeTexts.TITLE,
            WakeTimeTexts.CONTINUE_BUTTON_LABEL,
            WakeTimeTexts.INVALID_TIME_MESSAGE,
            WakeTimeTexts.UNKNOWN_ZONE_MESSAGE,
            WakeTimeTexts.SESSION_IN_PROGRESS_MESSAGE,
            WakeTimeTexts.BLOCKING_SECTION_TITLE,
            WakeTimeTexts.BLOCKING_NOW_LABEL,
            WakeTimeTexts.BLOCKING_AT_LABEL,
            WakeTimeTexts.BLOCKING_CONFIRM_LABEL,
            WakeTimeTexts.BLOCKING_DISMISS_LABEL,
            WakeTimeTexts.BLOCKING_IMMEDIATE_SENTENCE,
            WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE,
        )

    @Test
    fun noTextUsesVouvoiement() {
        allTexts.forEach { text ->
            assertThat(text.lowercase()).doesNotContain("vous")
            assertThat(text.lowercase()).doesNotContain("votre")
        }
    }

    @Test
    fun labelsAreNotEmpty() {
        allTexts.forEach { text -> assertThat(text).isNotEmpty() }
    }

    /** Textes imposés mot pour mot par SPEC_ANDROID §15, « Écran 5 — début du blocage » (Lot 6). */
    @Test
    fun theBlockingSectionUsesTheWordingOfTheSpecification() {
        assertThat(WakeTimeTexts.BLOCKING_SECTION_TITLE).isEqualTo("Blocage des applications")
        assertThat(WakeTimeTexts.BLOCKING_NOW_LABEL).isEqualTo("Maintenant")
        assertThat(WakeTimeTexts.BLOCKING_AT_LABEL).isEqualTo("À partir de")
        assertThat(WakeTimeTexts.BLOCKING_IMMEDIATE_SENTENCE)
            .isEqualTo("Tes applications seront bloquées dès l'activation.")
        assertThat(WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE)
            .isEqualTo(
                "L'heure de début du blocage doit être avant ton réveil. " +
                    "Pour bloquer tout de suite, choisis « Maintenant ».",
            )
    }

    /**
     * La phrase de confirmation différée reprend le libellé relatif, la date complète et le fuseau
     * du réveil (§15), en minuscule initiale puisqu'elle continue une phrase commencée.
     */
    @Test
    fun theDeferredSentenceLowercasesOnlyTheFirstLetterOfTheDisplay() {
        val paris = ZoneId.of("Europe/Paris")
        val now = ZonedDateTime.of(2026, 9, 14, 20, 0, 0, 0, paris).toInstant().toEpochMilli()
        val startsAt = ZonedDateTime.of(2026, 9, 14, 22, 30, 0, 0, paris).toInstant().toEpochMilli()
        val display =
            WakeScheduleFormatter.format(
                WakeScheduleDto(
                    localDateIso = "2026-09-14",
                    localTimeIso = "22:30",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = startsAt,
                ),
                nowEpochMillis = now,
            )

        assertThat(WakeTimeTexts.blockingDeferredSentence(display))
            .isEqualTo("Tes applications seront bloquées aujourd'hui, lundi 14 septembre à 22:30 (Europe/Paris).")
    }

    /** Le nom du fuseau ne doit pas être écrasé par la décapitalisation de la première lettre. */
    @Test
    fun theDeferredSentenceKeepsTheZoneNameIntact() {
        val auckland = ZoneId.of("Pacific/Auckland")
        val now = ZonedDateTime.of(2026, 9, 14, 20, 0, 0, 0, auckland).toInstant().toEpochMilli()
        val startsAt = ZonedDateTime.of(2026, 9, 14, 22, 30, 0, 0, auckland).toInstant().toEpochMilli()
        val display =
            WakeScheduleFormatter.format(
                WakeScheduleDto(
                    localDateIso = "2026-09-14",
                    localTimeIso = "22:30",
                    zoneIdAtActivation = "Pacific/Auckland",
                    triggerAtEpochMillis = startsAt,
                ),
                nowEpochMillis = now,
            )

        assertThat(WakeTimeTexts.blockingDeferredSentence(display)).contains("(Pacific/Auckland)")
    }
}
