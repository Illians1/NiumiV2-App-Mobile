package com.niumi.feature.setup.onboarding

/**
 * Limites à énoncer avant la première activation. SPEC_ANDROID §3 (dernier point) en fait une
 * obligation : « Cette limite est intentionnelle et doit être expliquée avant la première
 * activation. » `object` pur, testable en JVM, même motif qu'`AccessibilityConsentTexts`.
 */
object OnboardingTexts {
    const val TITLE = "Avant ta première session"

    const val INTRO =
        "Niumi te réveille et bloque les applications que tu choisis jusqu'au scan de ton " +
            "boîtier. Voilà ce qu'il ne peut pas garantir."

    /**
     * Six limites, une par contrainte documentée : arrêt forcé (§4.2), scan sur écran verrouillé
     * (§4.4), désactivation du service d'accessibilité (§4.3), absence de secours logiciel (§3,
     * §4.5), réinitialisation de l'exemption d'énergie par une mise à jour (§13), et avertissement
     * jamais garanti immédiat (§13.1).
     */
    val limits =
        listOf(
            "Un arrêt forcé depuis les réglages supprime le réveil programmé : Niumi ne sonnera pas.",
            "Le scan sur écran verrouillé n'est pas garanti : ton téléphone peut exiger un " +
                "déverrouillage avant de lire le boîtier.",
            "Le service d'accessibilité peut être désactivé à tout moment dans les réglages " +
                "Android, ce qui arrête le blocage.",
            "Il n'existe aucun secours logiciel pendant une session : ni code, ni délai, ni " +
                "bouton « Arrêter quand même ». Sans ton boîtier, il te reste l'arrêt forcé ou " +
                "l'extinction du téléphone.",
            "Une mise à jour de Niumi peut réinitialiser l'exemption d'énergie. Le diagnostic la " +
                "revérifie avant chaque session.",
            "Si un réglage casse ton réveil après l'activation, Niumi t'avertit au plus tôt, " +
                "jamais immédiatement : le système peut l'avoir arrêté entre-temps.",
        )

    const val ACKNOWLEDGEMENT_LABEL = "J'ai compris"

    const val CONTINUE_BUTTON_LABEL = "Continuer"
}
