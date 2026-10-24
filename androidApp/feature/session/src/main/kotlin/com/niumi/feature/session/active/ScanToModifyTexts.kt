package com.niumi.feature.session.active

import com.niumi.system.nfc.NfcAvailability

/**
 * Textes des écrans 9 et 11 (SPEC_ANDROID §15). §15 nomme ces écrans sans en fixer les libellés :
 * ceux marqués « imposé » viennent du plan de l'étape 15 et sont repris mot pour mot. Tutoiement
 * partout (§15).
 */
object ScanToModifyTexts {
    /** Imposé. Aucune autre action n'existe sur cet écran (SPEC_ANDROID §3, §10.2). */
    const val INVITATION =
        "Scanne ton Niumi Spot pour annuler ou modifier ta session. " +
            "Tes applications resteront bloquées jusqu'au scan."

    const val TITLE = "Scan requis"

    /**
     * §11.2 : un Niumi Spot non enregistré au moment du scan ne change aucun état (SPEC_CORE_KMP §4).
     * Texte de Mehdi du 2026-10-24 (étape 43) : l'ancien « n'est pas celui de ta session » ne tenait
     * plus, les Niumi Spots n'étant plus figés dans la session.
     */
    const val UNKNOWN_BOX = "Ce Niumi Spot n'est pas connu. Rien n'a été modifié."

    /** §11.2 : texte dédié au tag physiquement illisible, distinct d'un Niumi Spot inconnu. */
    const val UNREADABLE = "Ce tag n'a pas pu être lu. Réessaie en le posant bien à plat contre le téléphone."

    const val RELEASING = "Déblocage en cours…"

    fun availabilityMessage(availability: NfcAvailability): String? =
        when (availability) {
            NfcAvailability.ENABLED -> null
            NfcAvailability.DISABLED -> "Le NFC est désactivé. Active-le pour scanner ton Niumi Spot."
            NfcAvailability.ABSENT -> "Ce téléphone n'a pas de NFC : le scan du Niumi Spot est impossible."
        }
}

/** Textes de l'écran 11 (SPEC_ANDROID §15, écran 11). Les deux libellés sont imposés par le plan. */
object CancelledTexts {
    const val TITLE = "Session annulée"

    const val BODY = "Tes applications sont débloquées."

    const val PREPARE_AGAIN_BUTTON = "Préparer un nouveau réveil"
}

/** Textes de l'écran 10 (SPEC_ANDROID §15, écran 10). Le corps est imposé par le plan. */
object CompletedTexts {
    const val TITLE = "Session terminée"

    /** Imposé. */
    const val BODY = "Tes applications sont débloquées."

    const val BACK_HOME_BUTTON = "Revenir à l'accueil"
}
