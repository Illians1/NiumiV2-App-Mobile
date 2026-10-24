package com.niumi.feature.setup.apps

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.AppSelectionSummary
import org.junit.Test

/**
 * Écran 4 (`design-plan/SPECIFICATION.md` §8, `TEXTES-DES-MAQUETTES.md` §10 à §12) : recopie mot
 * pour mot, avec l'apostrophe typographique (D13). Les textes que les maquettes ne dessinent pas
 * (limite, états vides) sont ceux validés par Mehdi le 2026-10-22.
 */
class AppSelectionTextsTest {
    @Test
    fun theScreenIsQuotedWordForWord() {
        assertThat(AppSelectionTexts.TITLE).isEqualTo("Quelles applications veux-tu bloquer ?")
        assertThat(AppSelectionTexts.BODY)
            .isEqualTo("Elles seront inaccessibles de l’heure du coucher jusqu’au scan de ton Niumi Spot.")
        assertThat(AppSelectionTexts.NONE_CHOSEN).isEqualTo("Aucune application choisie")
        assertThat(AppSelectionTexts.CHOOSE).isEqualTo("Choisir les applications")
        assertThat(AppSelectionTexts.MODIFY).isEqualTo("Modifier")
        assertThat(AppSelectionTexts.HINT_EMPTY).isEqualTo("Choisis au moins une application.")
        assertThat(AppSelectionTexts.HINT_CHOSEN).isEqualTo("Tu pourras changer ce choix plus tard, depuis l’accueil.")
        assertThat(AppSelectionTexts.CONTINUE).isEqualTo("Continuer")
    }

    @Test
    fun theSheetIsQuotedWordForWord() {
        assertThat(AppSelectionTexts.PAGE_TITLE).isEqualTo("Applications à bloquer")
        assertThat(AppSelectionTexts.CLOSE_LABEL).isEqualTo("Fermer sans enregistrer")
        assertThat(AppSelectionTexts.SEARCH).isEqualTo("Rechercher")
        assertThat(AppSelectionTexts.SEARCH_LABEL).isEqualTo("Rechercher une application")
        assertThat(AppSelectionTexts.VALIDATE).isEqualTo("Valider")
    }

    /** Pleine page et section en tête, décisions de Mehdi du 2026-10-22. */
    @Test
    fun theSectionsOfThePageUseTheTextsChosenOnOctober22() {
        assertThat(AppSelectionTexts.CHOSEN_SECTION).isEqualTo("Applications déjà choisies")
        assertThat(AppSelectionTexts.ALL_SECTION).isEqualTo("Toutes les applications")
    }

    /** Libellés lus par TalkBack, sans texte à l'écran (décisions du 2026-10-22). */
    @Test
    fun theLoadingAndClearLabelsUseTheTextsChosenOnOctober22() {
        assertThat(AppSelectionTexts.LOADING).isEqualTo("Chargement des applications")
        assertThat(AppSelectionTexts.CLEAR_SEARCH).isEqualTo("Effacer la recherche")
    }

    /** États non dessinés, textes validés par Mehdi le 2026-10-22. */
    @Test
    fun theUndrawnStatesUseTheTextsChosenOnOctober22() {
        assertThat(AppSelectionTexts.NO_MATCH).isEqualTo("Aucune application ne correspond.")
        assertThat(AppSelectionTexts.NO_APPS).isEqualTo("Aucune application à bloquer sur ce téléphone.")
        assertThat(AppSelectionTexts.limitReached()).isEqualTo("50 applications choisies, c’est le maximum.")
    }

    /** La limite écrite vient du moteur commun, jamais d'un nombre recopié dans l'écran. */
    @Test
    fun theLimitSentenceFollowsTheSharedRule() {
        assertThat(AppSelectionTexts.limitReached()).startsWith("${AppSelectionSummary.MAX_COUNT} ")
    }

    @Test
    fun theCounterAgreesWithItsNumber() {
        assertThat(AppSelectionTexts.chosen(0)).isEqualTo(AppSelectionTexts.NONE_CHOSEN)
        assertThat(AppSelectionTexts.chosen(1)).isEqualTo("1 application choisie")
        assertThat(AppSelectionTexts.chosen(6)).isEqualTo("6 applications choisies")
        assertThat(AppSelectionTexts.chosen(50)).isEqualTo("50 applications choisies")
    }

    @Test
    fun theIconsLabelCountsTheChosenApps() {
        assertThat(AppSelectionTexts.iconsLabel(6)).isEqualTo("Icônes des 6 applications choisies")
        assertThat(AppSelectionTexts.iconsLabel(1)).isEqualTo("Icône de l’application choisie")
        assertThat(AppSelectionTexts.moreApps(9)).isEqualTo("+9")
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
                AppSelectionTexts.TITLE,
                AppSelectionTexts.BODY,
                AppSelectionTexts.NONE_CHOSEN,
                AppSelectionTexts.CHOOSE,
                AppSelectionTexts.MODIFY,
                AppSelectionTexts.HINT_EMPTY,
                AppSelectionTexts.HINT_CHOSEN,
                AppSelectionTexts.CONTINUE,
                AppSelectionTexts.PAGE_TITLE,
                AppSelectionTexts.CLOSE_LABEL,
                AppSelectionTexts.SEARCH,
                AppSelectionTexts.SEARCH_LABEL,
                AppSelectionTexts.VALIDATE,
                AppSelectionTexts.CHOSEN_SECTION,
                AppSelectionTexts.ALL_SECTION,
                AppSelectionTexts.LOADING,
                AppSelectionTexts.CLEAR_SEARCH,
                AppSelectionTexts.NO_MATCH,
                AppSelectionTexts.NO_APPS,
                AppSelectionTexts.limitReached(),
                AppSelectionTexts.chosen(1),
                AppSelectionTexts.chosen(6),
                AppSelectionTexts.iconsLabel(1),
                AppSelectionTexts.iconsLabel(6),
            )
    }
}
