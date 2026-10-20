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
}
