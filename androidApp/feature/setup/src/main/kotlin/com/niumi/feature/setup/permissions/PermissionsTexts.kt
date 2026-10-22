package com.niumi.feature.setup.permissions

/**
 * Textes de l'écran 2 · Autorisations (`design-plan/SPECIFICATION.md` §5, `TEXTES-DES-MAQUETTES.md`
 * §03), recopiés mot pour mot avec l'apostrophe typographique (D13). Vérifiés par
 * `PermissionsTextsTest`. Introduction, titre et tuiles « Batterie » et « Verrouiller Niumi »
 * revus par Mehdi le 2026-10-21 ; la maquette 03 montre encore les anciens. Le libellé « Voir
 * comment faire » du bouton « ? » attend le guide (M1, reporté le 2026-10-20).
 */
object PermissionsTexts {
    /**
     * Le titre compte les tuiles affichées : cinq sur HyperOS, quatre ailleurs sur Android. Les
     * nombres sont ceux qu'écrit le titre, pas des réglages.
     */
    @Suppress("MagicNumber")
    fun title(tileCount: Int): String =
        when (tileCount) {
            3 -> "Trois autorisations"

            4 -> "Quatre autorisations"

            5 -> "Cinq autorisations"

            // Aucun autre compte n'existe sur Android ; un titre sans nombre ne ment jamais.
            else -> "Autorisations"
        }

    const val BODY = "Sans elles, Niumi ne peut pas fonctionner."

    const val ALARM_NAME = "Alarme"

    const val ALARM_PHRASE = "Pour que ton réveil sonne à l’heure, même téléphone verrouillé."

    const val BLOCKING_NAME = "Blocage des applications"

    const val BLOCKING_PHRASE = "Pour bloquer les applications que tu choisis, et seulement celles-là."

    const val BATTERY_NAME = "Batterie"

    const val BATTERY_PHRASE = "Pour que le téléphone n’arrête pas Niumi pendant la nuit."

    const val RECENTS_LOCK_NAME = "Verrouiller Niumi"

    const val RECENTS_LOCK_PHRASE = "Pour que Niumi reste actif quand tu vides les applications récentes."

    const val NOTIFICATIONS_NAME = "Notifications"

    const val NOTIFICATIONS_PHRASE = "Pour pouvoir te tenir informé de tout ce qui est important."

    const val AUTHORIZE = "Autoriser"

    const val CONTINUE = "Continuer"

    /** Libellé d'accessibilité de la flèche. */
    const val BACK = "Retour"

    /** Libellé d'accessibilité de la coche d'une tuile accordée. */
    const val GRANTED = "Accordée"

    /** Libellé d'accessibilité de la croix des fenêtres (le mot des maquettes 04, 11, 16). */
    const val CLOSE = "Fermer"
}
