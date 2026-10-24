package com.niumi.feature.ringing.ui

import com.niumi.designsystem.component.NfcScanSheetContent
import com.niumi.system.nfc.ScanFailureReason

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

    /** Nom de la feuille d'échec pour TalkBack (`aria-label` des maquettes 30 à 33). */
    const val FAILURE_PANE = "Échec du scan"

    private const val NOT_A_NIUMI_SPOT = "Ce n’est pas un Niumi Spot."
    private const val NOT_REGISTERED = "Ce Niumi Spot n’est pas enregistré."
    private const val SCAN_A_REGISTERED_POINT = "Scanne un Niumi Spot que tu as enregistré."
    private const val UNREADABLE = "Ton Niumi Spot n’a pas pu être lu."
    private const val UNREADABLE_HELP = "Rapproche ton téléphone et réessaie."
    private const val NFC_DISABLED = "Le NFC est coupé."
    private const val NFC_DISABLED_HELP = "Active-le pour scanner ton Niumi Spot."
    private const val RETRY = "Réessayer"
    private const val OPEN_SETTINGS = "Ouvrir les réglages"

    /** Lu par TalkBack sur le cercle de chargement, après un scan validé (aucun texte à l'écran). */
    const val RELEASE_LOADER = "Fin de la session"

    const val NFC_ABSENT = "Cet appareil ne prend pas en charge le NFC."

    /** Titre, phrase d'aide et bouton de chaque échec (`SPECIFICATION.md` §14, tableau des échecs). */
    fun failure(reason: ScanFailureReason): NfcScanSheetContent.Failure =
        when (reason) {
            ScanFailureReason.NOT_A_NIUMI_SPOT -> {
                NfcScanSheetContent.Failure(NOT_A_NIUMI_SPOT, SCAN_A_REGISTERED_POINT, RETRY)
            }

            ScanFailureReason.NOT_REGISTERED -> {
                NfcScanSheetContent.Failure(NOT_REGISTERED, SCAN_A_REGISTERED_POINT, RETRY)
            }

            ScanFailureReason.UNREADABLE -> {
                NfcScanSheetContent.Failure(UNREADABLE, UNREADABLE_HELP, RETRY)
            }

            ScanFailureReason.NFC_DISABLED -> {
                NfcScanSheetContent.Failure(NFC_DISABLED, NFC_DISABLED_HELP, OPEN_SETTINGS)
            }
        }
}
