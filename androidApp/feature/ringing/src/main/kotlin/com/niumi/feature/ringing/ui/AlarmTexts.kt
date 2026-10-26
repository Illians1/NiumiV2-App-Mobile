package com.niumi.feature.ringing.ui

import com.niumi.designsystem.component.NfcScanSheetContent
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanFailureTexts

/**
 * Textes de l'écran 10 · Réveil et de ses feuilles d'échec (étape 38), recopiés de
 * `design-plan/SPECIFICATION.md` §14 et de `TEXTES-DES-MAQUETTES.md` §28 à §33, apostrophe
 * typographique (D13). La feuille « Prêt à scanner » est portée par le design system
 * (`NiumiComponentTexts`), la même aux écrans 3, 10 et à l'annulation.
 *
 * Le lien « Je n'ai pas accès à mon Niumi Spot » n'est pas encore là : il arrive avec la sortie de
 * secours (étape 49).
 *
 * Non dessinés, validés par Mehdi le 2026-10-22 : le libellé TalkBack du chargement qui suit un
 * scan validé, et l'absence de NFC (cas défensif : le manifeste exige le NFC). Vérifié par
 * `AlarmTextsTest`.
 */
object AlarmTexts {
    /**
     * Décision de Mehdi du 2026-10-22, à la place de « Scanne ton Niumi Spot » (§14) : le scan ne
     * marche qu'après « Scanner le Niumi Spot » (Q4), la consigne parle donc du déplacement.
     */
    const val INSTRUCTION = "Rejoins ton Niumi Spot"
    const val SCAN_BUTTON = "Scanner le Niumi Spot"

    /** Libellé d'accessibilité du dessin (source de la maquette 28). */
    const val DRAWING = "Un téléphone approché d’un Niumi Spot"

    /** Nom de la feuille d'échec pour TalkBack, partagé avec la feuille d'annulation. */
    const val FAILURE_PANE = ScanFailureTexts.FAILURE_PANE

    /** Lu par TalkBack sur le cercle de chargement, après un scan validé (aucun texte à l'écran). */
    const val RELEASE_LOADER = "Fin du réveil"

    const val NFC_ABSENT = "Cet appareil ne prend pas en charge le NFC."

    /** Titre, phrase d'aide et bouton de chaque échec (`ScanFailureTexts`, depuis l'étape 46). */
    fun failure(reason: ScanFailureReason): NfcScanSheetContent.Failure =
        ScanFailureTexts.of(reason).let { NfcScanSheetContent.Failure(it.title, it.help, it.actionLabel) }
}
