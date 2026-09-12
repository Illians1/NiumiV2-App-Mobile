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
        "Scanne ton boîtier Niumi pour annuler ou modifier ta session. " +
            "Tes applications resteront bloquées jusqu'au scan."

    const val TITLE = "Scan requis"

    /** §11.2 : un boîtier qui n'est pas celui de la session ne change aucun état (SPEC_CORE_KMP §4). */
    const val UNKNOWN_BOX = "Ce boîtier n'est pas celui de ta session. Rien n'a été modifié."

    /** §11.2 : texte dédié au tag physiquement illisible, distinct d'un boîtier inconnu. */
    const val UNREADABLE = "Ce tag n'a pas pu être lu. Réessaie en le posant bien à plat contre le téléphone."

    const val RELEASING = "Déblocage en cours…"

    fun availabilityMessage(availability: NfcAvailability): String? =
        when (availability) {
            NfcAvailability.ENABLED -> null
            NfcAvailability.DISABLED -> "Le NFC est désactivé. Active-le pour scanner ton boîtier."
            NfcAvailability.ABSENT -> "Ce téléphone n'a pas de NFC : le scan du boîtier est impossible."
        }
}

/** Textes de l'écran 11 (SPEC_ANDROID §15, écran 11). Les deux libellés sont imposés par le plan. */
object CancelledTexts {
    const val TITLE = "Session annulée"

    const val BODY = "Tes applications sont débloquées."

    const val PREPARE_AGAIN_BUTTON = "Préparer un nouveau réveil"
}
