package com.niumi.feature.setup.pairing

import com.niumi.database.pairing.PairedBoxName

/**
 * Textes de l'écran 3 « Mes boîtiers » (SPEC_ANDROID §15 Lot 8, §11.1). Tutoiement partout.
 * `object` pur, testable en JVM, même motif que `PresentationTexts`.
 *
 * Aucun texte ne mentionne le token ni son empreinte : §16 interdit de les afficher comme de les
 * journaliser. Seul un préfixe de `boxId` est montré, pour que l'utilisateur distingue deux
 * boîtiers sans exposer d'identifiant complet.
 */
object PairingTexts {
    const val TITLE = "Mes boîtiers"

    const val INSTRUCTION = "Approche ton boîtier Niumi du dos de ton téléphone."

    const val NFC_DISABLED = "Le NFC est désactivé. Active-le pour associer ton boîtier."

    const val NFC_ABSENT = "Ce téléphone n'a pas de NFC. Niumi ne peut pas fonctionner sans boîtier."

    const val OPEN_NFC_SETTINGS_BUTTON_LABEL = "Ouvrir les réglages NFC"

    const val UNREADABLE = "Tag illisible. Réessaie en approchant le boîtier plus lentement."

    const val UNKNOWN_PAYLOAD = "Ce tag n'est pas un boîtier Niumi."

    /** Lot 8 : remplace le dialogue de remplacement — un boîtier déjà associé n'est pas réassocié. */
    const val ALREADY_PAIRED = "Ce boîtier est déjà associé."

    /**
     * Affiché à la place de [ADD_BUTTON_LABEL] dès dix boîtiers (`PairedBoxLimit.MAX`, contrat KMP
     * 1.5). Texte transitoire : l'écran disparaît à l'étape 47.
     */
    const val LIMIT_REACHED = "10 boîtiers maximum."

    const val BOX_ADDED = "Boîtier ajouté."

    const val CONTINUE_BUTTON_LABEL = "Continuer"

    const val ADD_BUTTON_LABEL = "Ajouter un boîtier"

    /** Arrête la lecture ouverte par [ADD_BUTTON_LABEL], et ferme un dialogue de nom sans écrire. */
    const val CANCEL_LABEL = "Annuler"

    const val RENAME_LABEL = "Renommer"

    const val REMOVE_LABEL = "Retirer"

    const val ADD_DIALOG_TITLE = "Nouveau boîtier"

    const val RENAME_DIALOG_TITLE = "Renommer le boîtier"

    const val NAME_FIELD_LABEL = "Nom"

    const val CONFIRM_ADD_LABEL = "Ajouter"

    const val CONFIRM_RENAME_LABEL = "Renommer"

    /** Préfixe de `boxId` affiché : assez pour distinguer deux boîtiers, jamais l'identifiant entier. */
    const val BOX_ID_PREFIX_LENGTH = 8

    /** Descriptions TalkBack des deux actions d'une ligne : « Renommer » seul ne dit pas lequel (§15). */
    fun renameDescription(name: String): String = "Renommer $name"

    fun removeDescription(name: String): String = "Retirer $name"

    /** Compteur du champ de nom, sur la longueur que le dépôt jugera : après `trim()` (§11.1). */
    fun nameCounter(text: String): String = "${text.trim().length}/${PairedBoxName.MAX_LENGTH}"

    fun prefixOf(boxId: String): String = "${boxId.take(BOX_ID_PREFIX_LENGTH)}…"
}
