package com.niumi.feature.setup.pairing

/**
 * Textes de l'écran 3 (SPEC_ANDROID §15, §11.1). Tutoiement partout. `object` pur, testable en
 * JVM, même motif qu'`OnboardingTexts`.
 *
 * Aucun texte ne mentionne le token ni son empreinte : §16 interdit de les afficher comme de les
 * journaliser. Seul un préfixe de `boxId` est montré, pour que l'utilisateur distingue deux
 * boîtiers sans exposer d'identifiant complet.
 */
object PairingTexts {
    const val TITLE = "Associe ton boîtier"

    const val INSTRUCTION = "Approche ton boîtier Niumi du dos de ton téléphone."

    const val NFC_DISABLED = "Le NFC est désactivé. Active-le pour associer ton boîtier."

    const val NFC_ABSENT = "Ce téléphone n'a pas de NFC. Niumi ne peut pas fonctionner sans boîtier."

    const val OPEN_NFC_SETTINGS_BUTTON_LABEL = "Ouvrir les réglages NFC"

    const val UNREADABLE = "Tag illisible. Réessaie en approchant le boîtier plus lentement."

    const val UNKNOWN_PAYLOAD = "Ce tag n'est pas un boîtier Niumi."

    const val REPLACE_QUESTION = "Remplacer le boîtier actuel ?"

    const val REPLACE_EXPLANATION =
        "Un seul boîtier peut être associé. L'ancien ne pourra plus terminer tes sessions."

    const val REPLACE_CONFIRM_BUTTON_LABEL = "Remplacer"

    const val REPLACE_CANCEL_BUTTON_LABEL = "Garder l'actuel"

    const val PAIRED = "Boîtier associé."

    const val CONTINUE_BUTTON_LABEL = "Continuer"

    const val SESSION_IN_PROGRESS =
        "Une session est en cours. Ton boîtier ne peut pas être remplacé avant sa fin."

    /** Préfixe de `boxId` affiché : assez pour distinguer deux boîtiers, jamais l'identifiant entier. */
    const val BOX_ID_PREFIX_LENGTH = 8

    fun currentBoxLabel(boxIdPrefix: String): String = "Boîtier associé : $boxIdPrefix…"
}
