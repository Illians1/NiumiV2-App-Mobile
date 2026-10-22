package com.niumi.feature.setup.apps

import com.niumi.core.domain.AppSelectionSummary

/**
 * Textes de l'écran 4 · Choix des applications et de sa page Android
 * (`design-plan/SPECIFICATION.md` §8, `TEXTES-DES-MAQUETTES.md` §10 à §12), recopiés mot pour mot
 * avec l'apostrophe typographique (D13). Vérifiés par `AppSelectionTextsTest`.
 *
 * Les maquettes ne dessinent ni la limite de 50 ni les états vides : leurs textes sont ceux validés
 * par Mehdi le 2026-10-22. La limite vient d'`AppSelectionSummary` (`:shared:core`), seule
 * autorité de la règle (SPEC_CORE_KMP §2 point 12).
 */
object AppSelectionTexts {
    const val TITLE = "Quelles applications veux-tu bloquer ?"

    const val BODY = "Elles seront inaccessibles de l’heure du coucher jusqu’au scan de ton Niumi Point."

    const val NONE_CHOSEN = "Aucune application choisie"

    const val CHOOSE = "Choisir les applications"

    const val MODIFY = "Modifier"

    const val HINT_EMPTY = "Choisis au moins une application."

    const val HINT_CHOSEN = "Tu pourras changer ce choix plus tard, depuis l’accueil."

    const val CONTINUE = "Continuer"

    const val PAGE_TITLE = "Applications à bloquer"

    /** Libellé d'accessibilité de la croix de la page. */
    const val CLOSE_LABEL = "Fermer sans enregistrer"

    /** Texte du champ vide. */
    const val SEARCH = "Rechercher"

    /** Libellé d'accessibilité du champ de recherche. */
    const val SEARCH_LABEL = "Rechercher une application"

    const val VALIDATE = "Valider"

    /** Section en tête de la grille : les applications cochées à l'ouverture (2026-10-22). */
    const val CHOSEN_SECTION = "Applications déjà choisies"

    /** Section qui suit, quand la première existe. */
    const val ALL_SECTION = "Toutes les applications"

    /** Libellé d'accessibilité de l'indicateur de chargement, sans texte à l'écran (2026-10-22). */
    const val LOADING = "Chargement des applications"

    /** Libellé d'accessibilité de la croix qui vide le champ de recherche (2026-10-22). */
    const val CLEAR_SEARCH = "Effacer la recherche"

    const val NO_MATCH = "Aucune application ne correspond."

    const val NO_APPS = "Aucune application à bloquer sur ce téléphone."

    /** Ligne du compteur de la page quand la limite commune est atteinte. */
    fun limitReached(): String = "${AppSelectionSummary.MAX_COUNT} applications choisies, c’est le maximum."

    /** Compteur de la carte et de la page ; singulier pour une seule application, aucune à zéro. */
    fun chosen(count: Int): String =
        when (count) {
            0 -> NONE_CHOSEN
            1 -> "1 application choisie"
            else -> "$count applications choisies"
        }

    /** Libellé d'accessibilité de la rangée d'icônes de la carte (maquette 12). */
    fun iconsLabel(count: Int): String =
        if (count == 1) "Icône de l’application choisie" else "Icônes des $count applications choisies"

    /** Dernière case de la carte quand les icônes ne tiennent pas toutes. */
    fun moreApps(hidden: Int): String = "+$hidden"
}
