package com.niumi.feature.setup.accessibility

/**
 * Contenu de l'écran de consentement (SPEC_ANDROID §12.3), avant toute ouverture des réglages
 * d'accessibilité. `object` pur, testable en JVM, consommé par
 * [AccessibilityConsentScreen][com.niumi.feature.setup.accessibility.AccessibilityConsentScreen].
 */
object AccessibilityConsentTexts {
    const val TITLE = "Autoriser le blocage des applications"

    // Les cinq points de SPEC_ANDROID §12.3, mot pour mot.
    val points =
        listOf(
            "Niumi observe le nom de l'application affichée.",
            "Cette information sert uniquement à renvoyer les applications choisies vers l'accueil.",
            "Niumi ne lit pas le contenu des écrans ou les saisies.",
            "Le service peut être désactivé dans les réglages Android.",
            "Une session ne peut pas être activée si le service est inactif.",
        )

    const val OPEN_SETTINGS_BUTTON_LABEL = "Ouvrir les réglages d'accessibilité"

    // Doit rester identique à niumi_accessibility_service_description (:feature:session,
    // res/values/strings.xml) : aucune dépendance feature-à-feature n'existe pour partager une
    // seule source ; les deux littéraux sont maintenus en synchronisation manuelle, documentée
    // aux deux endroits (voir ETAPE-05.md).
    const val SETTINGS_DESCRIPTION =
        "Observe le nom de l'application affichée pour ramener les applications bloquées " +
            "à l'accueil pendant une session Niumi. Ne lit ni le contenu des écrans ni les saisies."
}
