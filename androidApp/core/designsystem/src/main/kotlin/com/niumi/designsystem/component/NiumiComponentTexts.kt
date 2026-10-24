package com.niumi.designsystem.component

/**
 * Seuls textes portés par les composants du design system, les mêmes dans toutes les maquettes :
 * le libellé du cadenas (`TEXTES-DES-MAQUETTES.md`), le texte de remplacement du logo et la
 * feuille « Prêt à scanner » ([NfcScanSheet], étape 38). Les
 * autres libellés d'accessibilité (« Retour aux réglages », « Fermer sans enregistrer »,
 * « Panneau 1 sur 2 »…) changent d'un écran à l'autre : chaque écran les passe depuis son propre
 * `Texts`. Vérifié par `NiumiComponentTextsTest`.
 */
object NiumiComponentTexts {
    /** Annoncé par TalkBack sur un cadenas et sur un champ verrouillé pendant la session. */
    const val LOCKED = "Verrouillé"

    /** Texte de remplacement du logo (`alt="niumi"` dans les sources des maquettes). */
    const val LOGO = "niumi"

    /**
     * Feuille de scan, la même aux écrans 3, 10 et à l'annulation (`SPECIFICATION.md` §6 et §14,
     * maquettes 06, 20, 29). Phrase de la colonne Android : « téléphone », jamais « iPhone ».
     */
    const val SCAN_SHEET_TITLE = "Prêt à scanner"
    const val SCAN_SHEET_PHRASE = "Approche ton téléphone du Niumi Spot"
    const val SCAN_SHEET_CANCEL = "Annuler"
}
