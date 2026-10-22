package com.niumi.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Les seuls textes portés par les composants, identiques dans toutes les maquettes : le libellé du
 * cadenas (`TEXTES-DES-MAQUETTES.md`, « Verrouillé », images 15, 18 à 20, 22 à 27) et le texte
 * de remplacement du logo (`alt="niumi"`, sources des images 01, 02, 05 à 07, 22, 41). Les autres
 * libellés (« Retour aux réglages », « Fermer sans enregistrer »…) varient d'un écran à l'autre et
 * viennent du `Texts` de chaque écran.
 */
class NiumiComponentTextsTest {
    @Test
    fun theLockIsAnnouncedWithTheWordOfTheMockups() {
        assertThat(NiumiComponentTexts.LOCKED).isEqualTo("Verrouillé")
    }

    @Test
    fun theLogoIsAnnouncedWithTheAltTextOfTheMockups() {
        assertThat(NiumiComponentTexts.LOGO).isEqualTo("niumi")
    }

    /**
     * Feuille « Prêt à scanner », la même aux écrans 3, 10 et à l'annulation (`SPECIFICATION.md` §6
     * et §14, maquettes 06, 20, 29) ; phrase de la colonne Android.
     */
    @Test
    fun theScanSheetIsQuotedWordForWord() {
        assertThat(NiumiComponentTexts.SCAN_SHEET_TITLE).isEqualTo("Prêt à scanner")
        assertThat(NiumiComponentTexts.SCAN_SHEET_PHRASE).isEqualTo("Approche ton téléphone du Niumi Point")
        assertThat(NiumiComponentTexts.SCAN_SHEET_CANCEL).isEqualTo("Annuler")
    }
}
