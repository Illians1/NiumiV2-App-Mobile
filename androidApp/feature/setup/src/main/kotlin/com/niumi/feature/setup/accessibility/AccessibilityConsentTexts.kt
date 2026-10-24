package com.niumi.feature.setup.accessibility

/**
 * Contenu de la fenêtre de consentement (SPEC_ANDROID §12.3), ouverte par « Autoriser » sur la
 * tuile « Blocage des applications » de l'écran 2 et toujours avant les réglages d'accessibilité
 * (décision du 2026-10-21 : fenêtre centrée sur l'écran 2). `object` pur, testable en JVM,
 * consommé par `PermissionsScreen`.
 */
object AccessibilityConsentTexts {
    const val TITLE = "Autoriser le blocage des applications"

    // Les cinq points de SPEC_ANDROID §12.3, mot pour mot, apostrophe typographique (D13).
    val points =
        listOf(
            "Niumi observe le nom de l’application affichée.",
            "Cette information sert uniquement à renvoyer les applications choisies vers l’accueil.",
            "Niumi ne lit pas le contenu des écrans ou les saisies.",
            "Le service peut être désactivé dans les réglages Android.",
            // Texte de Mehdi du 2026-10-22, à la place de « Une session ne peut pas être activée si
            // le service est inactif. ».
            "Sans cette autorisation, Niumi ne peut pas fonctionner.",
        )

    /**
     * Le chemin à suivre une fois les réglages ouverts (décision de Mehdi du 2026-10-22). Aucun
     * libellé d'écran : « Applications téléchargées » et « Utiliser Niumi » sont ceux d'Android,
     * mais les constructeurs les renomment, et un libellé faux perdrait l'utilisateur.
     */
    const val HOW_TO = "Dans les réglages, trouve Niumi et active-le."

    const val OPEN_SETTINGS_BUTTON_LABEL = "Ouvrir les réglages d’accessibilité"

    // Doit rester identique à niumi_accessibility_service_description (:feature:session,
    // res/values/strings.xml) : aucune dépendance feature-à-feature n'existe pour partager une
    // seule source ; les deux littéraux sont maintenus en synchronisation manuelle, documentée
    // aux deux endroits (voir ETAPE-05.md).
    const val SETTINGS_DESCRIPTION =
        "Observe le nom de l'application affichée pour ramener les applications bloquées " +
            "à l'accueil tant qu'un réveil Niumi est programmé. Ne lit ni le contenu des écrans ni les saisies."
}
