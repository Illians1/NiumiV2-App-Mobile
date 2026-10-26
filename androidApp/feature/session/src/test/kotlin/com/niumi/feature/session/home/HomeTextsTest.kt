package com.niumi.feature.session.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Textes de l'accueil (`SPECIFICATION.md` §9, `TEXTES-DES-MAQUETTES.md` 15 à 17, maquettes 18 à 20
 * du 2026-10-25 ; étape 45), mot pour mot, tutoiement, apostrophe typographique (D13).
 */
class HomeTextsTest {
    @Test
    fun theDialAndCardTextsAreQuotedWordForWord() {
        assertThat(HomeTexts.BEDTIME).isEqualTo("Coucher")
        assertThat(HomeTexts.WAKE).isEqualTo("Réveil")
        assertThat(HomeTexts.WAKE_IN).isEqualTo("Réveil dans")
        assertThat(HomeTexts.DISABLED).isEqualTo("Désactivé")
        assertThat(HomeTexts.WAKE_ENABLED).isEqualTo("Réveil activé")
        assertThat(HomeTexts.WAKE_DISABLED).isEqualTo("Réveil désactivé")
        assertThat(HomeTexts.NOTHING_PLANNED).isEqualTo("Aucun réveil ni blocage prévu")
        assertThat(HomeTexts.BLOCKING_IN_PROGRESS).isEqualTo("Blocage en cours")
        assertThat(HomeTexts.BLOCKED_UNTIL_SCAN).isEqualTo("Applications bloquées jusqu’au scan de ton Niumi Spot")
        assertThat(HomeTexts.CANCEL_WAKE).isEqualTo("Annuler le réveil")
        assertThat(HomeTexts.LOCKED_UNTIL_SCAN).isEqualTo("Modifiable après avoir scanné ton Niumi Spot.")
    }

    @Test
    fun theCountdownsUseTheSharedTimeFormat() {
        assertThat(HomeTexts.blockingIn(56)).isEqualTo("Blocage des apps dans 56 min")
        assertThat(HomeTexts.blockingIn(86)).isEqualTo("Blocage des apps dans 1 h 26")
        assertThat(HomeTexts.sleep(510)).isEqualTo("8 h 30 de sommeil")
    }

    /** Période de 24 h (décision du 2026-10-25) : texte non dessiné, à valider par Mehdi. */
    @Test
    fun aTwentyFourHourPeriodIsNamedAsSuch() {
        assertThat(HomeTexts.sleep(1440)).isEqualTo("24 h de blocage")
        assertThat(HomeTexts.countdown(566)).isEqualTo("9 h 26")
    }

    /** Au-delà d'un jour, jours et heures (décision de Mehdi du 2026-10-25). */
    @Test
    fun beyondADayTheCountdownsReadDaysAndHours() {
        assertThat(HomeTexts.countdown(1440)).isEqualTo("1 j")
        assertThat(HomeTexts.countdown(1440 + 4 * 60 + 30)).isEqualTo("1 j 4 h")
        assertThat(HomeTexts.countdown(2 * 1440 + 4 * 60)).isEqualTo("2 j 4 h")
        assertThat(HomeTexts.countdown(6 * 1440)).isEqualTo("6 j")
        assertThat(HomeTexts.blockingIn(1440 + 60)).isEqualTo("Blocage des apps dans 1 j 1 h")
    }

    @Test
    fun theDayTabsAndChipsAreQuotedWordForWord() {
        assertThat(HomeTexts.ONE_OFF).isEqualTo("Ponctuel")
        assertThat(HomeTexts.DAILY).isEqualTo("Quotidien")
        assertThat(HomeTexts.DAY_LABELS).containsExactly("L", "M", "M", "J", "V", "S", "D").inOrder()
        assertThat(HomeTexts.DAY_NAMES)
            .containsExactly("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
            .inOrder()
    }

    /**
     * Titre des pastilles (décision de Mehdi du 2026-10-26) : un jour coché est un jour de réveil.
     * Texte non dessiné, à valider au premier essai.
     */
    @Test
    fun theDayChipsAreTitledAsWakeDays() {
        assertThat(HomeTexts.WAKE_DAYS).isEqualTo("Jours de réveil")
    }

    @Test
    fun theRowsAreQuotedWordForWord() {
        assertThat(HomeTexts.BLOCKED_APPS).isEqualTo("Applications bloquées")
        assertThat(HomeTexts.RINGTONE).isEqualTo("Sonnerie")
        assertThat(HomeTexts.iconsLabel(6)).isEqualTo("Icônes des 6 applications bloquées")
        assertThat(HomeTexts.iconsLabel(1)).isEqualTo("Icône de l’application bloquée")
        assertThat(HomeTexts.moreApps(4)).isEqualTo("+4")
    }

    @Test
    fun theTimePickerTextsAreQuotedWordForWord() {
        assertThat(HomeTexts.BEDTIME_TITLE).isEqualTo("Heure de coucher")
        assertThat(HomeTexts.WAKE_TITLE).isEqualTo("Heure de réveil")
        assertThat(HomeTexts.CONFIRM).isEqualTo("Valider")
        assertThat(HomeTexts.CLOSE).isEqualTo("Fermer")
        assertThat(HomeTexts.sleepBeforeWake(510, "7:00")).isEqualTo("8 h 30 de sommeil avant le réveil de 7:00")
        assertThat(HomeTexts.sleepAfterBedtime(510, "22:30")).isEqualTo("8 h 30 de sommeil après le coucher de 22:30")
    }

    /** Textes d'accessibilité et de la période de 24 h, non écrits dans les maquettes. */
    @Test
    fun theTextsNotWrittenByTheMockupsAreQuotedWordForWord() {
        assertThat(HomeTexts.CHOOSE_TIME).isEqualTo("Choisir l’heure")
        assertThat(HomeTexts.HOURS).isEqualTo("Heures")
        assertThat(HomeTexts.MINUTES).isEqualTo("Minutes")
        assertThat(HomeTexts.sleepBeforeWake(1440, "7:00")).isEqualTo("24 h de blocage jusqu’au réveil de 7:00")
        assertThat(HomeTexts.sleepAfterBedtime(1440, "22:30")).isEqualTo("24 h de blocage depuis le coucher de 22:30")
    }

    @Test
    fun noTextCarriesAStraightApostrophe() {
        assertThat(allTexts.filter { it.contains('\'') }).isEmpty()
    }

    @Test
    fun noTextSwitchesToTheFormalAddress() {
        assertThat(allTexts.filter { FORMAL_ADDRESS.containsMatchIn(it) }).isEmpty()
    }

    private companion object {
        val FORMAL_ADDRESS = Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""")

        val allTexts =
            listOf(
                HomeTexts.BEDTIME,
                HomeTexts.WAKE,
                HomeTexts.WAKE_IN,
                HomeTexts.DISABLED,
                HomeTexts.WAKE_ENABLED,
                HomeTexts.WAKE_DISABLED,
                HomeTexts.NOTHING_PLANNED,
                HomeTexts.BLOCKING_IN_PROGRESS,
                HomeTexts.BLOCKED_UNTIL_SCAN,
                HomeTexts.CANCEL_WAKE,
                HomeTexts.LOCKED_UNTIL_SCAN,
                HomeTexts.ONE_OFF,
                HomeTexts.DAILY,
                HomeTexts.WAKE_DAYS,
                HomeTexts.BLOCKED_APPS,
                HomeTexts.RINGTONE,
                HomeTexts.BEDTIME_TITLE,
                HomeTexts.WAKE_TITLE,
                HomeTexts.CONFIRM,
                HomeTexts.CLOSE,
                HomeTexts.CHOOSE_TIME,
                HomeTexts.IMMEDIATE_BLOCKING_TITLE,
                HomeTexts.immediateBlockingBody("22:30"),
                HomeTexts.BLOCK_NOW,
                HomeTexts.CANCEL,
                HomeTexts.HOURS,
                HomeTexts.MINUTES,
                HomeTexts.iconsLabel(1),
                HomeTexts.iconsLabel(6),
                HomeTexts.blockingIn(56),
                HomeTexts.sleep(1440),
                HomeTexts.sleepBeforeWake(1440, "7:00"),
                HomeTexts.sleepAfterBedtime(1440, "22:30"),
            ) + HomeTexts.DAY_NAMES
    }

    /** Confirmation avant un blocage immédiat (décision du 2026-10-25) : textes non dessinés, à valider. */
    @Test
    fun theImmediateBlockingConfirmationIsQuotedWordForWord() {
        assertThat(HomeTexts.IMMEDIATE_BLOCKING_TITLE).isEqualTo("Tes applications seront bloquées dès maintenant")
        assertThat(HomeTexts.immediateBlockingBody("22:30")).isEqualTo(
            "Ton coucher de 22:30 est déjà passé. Tes applications resteront bloquées jusqu’au scan de ton Niumi Spot.",
        )
        assertThat(HomeTexts.BLOCK_NOW).isEqualTo("Bloquer maintenant")
        assertThat(HomeTexts.CANCEL).isEqualTo("Annuler")
    }
}
