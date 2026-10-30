package com.niumi.feature.session.home

/**
 * Fenêtre « Annuler le réveil ? » (étape 46), recopiée de `design-plan/SPECIFICATION.md` §11 et de
 * `TEXTES-DES-MAQUETTES.md` §25 et §26 (D23 : « réveil », jamais « session » ; les sources
 * régénérées le 2026-10-25 reviennent à « session » par régression, E14). Les textes d'échec du
 * scan sont ceux de l'écran 10 (`ScanFailureTexts`). Le lien « Je n'ai pas accès à mon Niumi
 * Spot » arrive avec la sortie de secours (étape 49). Vérifié par `CancelSessionTextsTest`.
 */
object CancelSessionTexts {
    const val TITLE = "Annuler le réveil ?"
    const val BODY = "Scanne ton Niumi Spot pour annuler. Tes applications seront débloquées et ton réveil désactivé."
    const val SCAN_BUTTON = "Scanner le Niumi Spot"

    /** Phrase de la feuille « Prêt à scanner » (« ton téléphone » sur Android, §11). */
    const val SCAN_PHRASE = "Approche ton téléphone du Niumi Spot pour annuler le réveil"

    /**
     * Lu par TalkBack sur le cercle de chargement de la libération, aucun texte à l'écran (choix de
     * Mehdi du 2026-10-26 ; non dessiné, pendant de « Fin du réveil » à l'écran 10).
     */
    const val RELEASE_LOADER = "Annulation du réveil"
}
