package com.niumi.feature.setup.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §15, « Lot 8 — Niumi Spots multiples » : les textes de l'écran 3 sont repris mot pour
 * mot de la spec. Un écart ici est un écart à la source de vérité, pas un détail de formulation.
 */
class PairingTextsTest {
    @Test
    fun theSpecTextsAreQuotedWordForWord() {
        assertThat(PairingTexts.TITLE).isEqualTo("Mes Niumi Spots")
        assertThat(PairingTexts.INSTRUCTION).isEqualTo("Approche ton Niumi Spot du dos de ton téléphone.")
        assertThat(PairingTexts.ADD_BUTTON_LABEL).isEqualTo("Ajouter un Niumi Spot")
        assertThat(PairingTexts.CANCEL_LABEL).isEqualTo("Annuler")
        assertThat(PairingTexts.RENAME_LABEL).isEqualTo("Renommer")
        assertThat(PairingTexts.REMOVE_LABEL).isEqualTo("Retirer")
        assertThat(PairingTexts.renameDescription("Cuisine")).isEqualTo("Renommer Cuisine")
        assertThat(PairingTexts.removeDescription("Cuisine")).isEqualTo("Retirer Cuisine")
        assertThat(PairingTexts.ADD_DIALOG_TITLE).isEqualTo("Nouveau Niumi Spot")
        assertThat(PairingTexts.RENAME_DIALOG_TITLE).isEqualTo("Renommer le Niumi Spot")
        assertThat(PairingTexts.NAME_FIELD_LABEL).isEqualTo("Nom")
        assertThat(PairingTexts.CONFIRM_ADD_LABEL).isEqualTo("Ajouter")
        assertThat(PairingTexts.CONFIRM_RENAME_LABEL).isEqualTo("Renommer")
        assertThat(PairingTexts.BOX_ADDED).isEqualTo("Niumi Spot ajouté.")
        assertThat(PairingTexts.ALREADY_PAIRED).isEqualTo("Ce Niumi Spot est déjà associé.")
        assertThat(PairingTexts.LIMIT_REACHED).isEqualTo("10 Niumi Spots maximum.")
        assertThat(PairingTexts.CONTINUE_BUTTON_LABEL).isEqualTo("Continuer")
    }

    /** §15 : « textes de 11.1 et 11.2 inchangés » pour un tag étranger, illisible ou un NFC indisponible. */
    @Test
    fun theReadingTextsAreUnchangedByTheLot8() {
        assertThat(PairingTexts.NFC_DISABLED)
            .isEqualTo("Le NFC est désactivé. Active-le pour associer ton Niumi Spot.")
        assertThat(PairingTexts.NFC_ABSENT)
            .isEqualTo("Ce téléphone n'a pas de NFC. Niumi ne peut pas fonctionner sans Niumi Spot.")
        assertThat(
            PairingTexts.UNREADABLE,
        ).isEqualTo("Tag illisible. Réessaie en approchant le Niumi Spot plus lentement.")
        assertThat(PairingTexts.UNKNOWN_PAYLOAD).isEqualTo("Ce tag n'est pas un Niumi Spot.")
    }

    @Test
    fun theNameCounterCountsTheTrimmedNameAgainstThirty() {
        assertThat(PairingTexts.nameCounter("  Cuisine ")).isEqualTo("7/30")
        assertThat(PairingTexts.nameCounter("")).isEqualTo("0/30")
    }

    /** §16 : seul un préfixe de huit caractères est montré, suivi d'une ellipse. */
    @Test
    fun theBoxPrefixIsEightCharactersFollowedByAnEllipsis() {
        assertThat(PairingTexts.prefixOf("550e8400-e29b-41d4-a716-446655440000")).isEqualTo("550e8400…")
    }
}
