package com.niumi.designsystem.component

/**
 * Seuls textes portés par les composants du design system, les mêmes dans toutes les maquettes :
 * le libellé du cadenas (`TEXTES-DES-MAQUETTES.md`) et le texte de remplacement du logo. Les
 * autres libellés d'accessibilité (« Retour aux réglages », « Fermer sans enregistrer »,
 * « Panneau 1 sur 2 »…) changent d'un écran à l'autre : chaque écran les passe depuis son propre
 * `Texts`. Vérifié par `NiumiComponentTextsTest`.
 */
object NiumiComponentTexts {
    /** Annoncé par TalkBack sur un cadenas et sur un champ verrouillé pendant la session. */
    const val LOCKED = "Verrouillé"

    /** Texte de remplacement du logo (`alt="niumi"` dans les sources des maquettes). */
    const val LOGO = "niumi"
}
