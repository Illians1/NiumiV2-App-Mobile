package com.niumi.feature.setup.presentation

/**
 * Textes de l'écran 1 · Présentation (`design-plan/SPECIFICATION.md` §4, `TEXTES-DES-MAQUETTES.md`
 * §01–02), recopiés mot pour mot avec l'apostrophe typographique (D13). Les deux titres s'écrivent
 * sans point final (D12, tranché le 2026-10-20). Vérifiés par `PresentationTextsTest`.
 */
object PresentationTexts {
    const val PANEL_1_TITLE = "À l’heure de te coucher, tes distractions sont bloquées"

    const val PANEL_1_BODY = "À l’heure que tu choisis, Niumi bloque les applications que tu as désignées."

    const val PANEL_2_TITLE = "Le matin, Niumi t’aide à te lever"

    const val PANEL_2_BODY =
        "Ton alarme sonne jusqu’à ce que tu scannes le Niumi Point situé dans la pièce de ton choix."

    const val CONTINUE = "Continuer"

    const val CONFIGURE = "Configurer Niumi"

    /** Libellé d'accessibilité de la flèche du panneau 2. */
    const val BACK = "Retour"

    /** Libellés d'accessibilité des dessins. */
    const val PANEL_1_DRAWING = "Un téléphone dont les applications sont verrouillées"
    const val PANEL_2_DRAWING = "Un téléphone approché d’un Niumi Point"

    /** Libellés d'accessibilité du repère à deux traits. */
    const val PANEL_1_INDICATOR = "Panneau 1 sur 2"
    const val PANEL_2_INDICATOR = "Panneau 2 sur 2"
}
