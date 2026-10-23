package com.niumi.feature.session.blocking

/**
 * Textes de l'écran 7 · Écran de blocage (étape 39), d'après `design-plan/SPECIFICATION.md` §12 et
 * la maquette 22. Sur Android, le titre nomme l'application (décision de Mehdi du 2026-10-23) :
 * « L’application YouTube est bloquée », accordé avec « application », juste quel que soit le genre
 * du nom (« Calculatrice est bloqué » ne l'était pas) ; la phrase du dessous ne donne pas de genre
 * non plus (« l’ouvrir »). Verrouillés par `BlockScreenTextsTest`.
 */
object BlockScreenTexts {
    /** Titre de la maquette, gardé quand le nom manque. */
    const val FALLBACK_TITLE = "Cette application est bloquée"
    const val BODY = "Tu pourras l’ouvrir après avoir scanné ton Niumi Point."
    const val CLOSE = "Fermer"
    const val OPEN_NIUMI = "Ouvrir Niumi"

    /** « L’application YouTube est bloquée », ou [FALLBACK_TITLE] sans nom. */
    fun title(appName: String?): String {
        val name = appName?.trim().orEmpty()
        return if (name.isEmpty()) FALLBACK_TITLE else "L’application $name est bloquée"
    }
}
