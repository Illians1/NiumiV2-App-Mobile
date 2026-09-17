package com.niumi.feature.session.wake

import com.niumi.feature.session.ui.WakeScheduleDisplay
import java.util.Locale

/** Textes de l'écran 5 (SPEC_ANDROID §15, écran 5), tutoiement partout. */
object WakeTimeTexts {
    const val TITLE = "À quelle heure veux-tu te réveiller ?"

    const val CONTINUE_BUTTON_LABEL = "Continuer"

    const val INVALID_TIME_MESSAGE = "Cette heure n'est pas valide. Choisis-en une autre."

    const val UNKNOWN_ZONE_MESSAGE =
        "Ton fuseau horaire n'a pas pu être reconnu. Vérifie les réglages de date et heure de " +
            "ton téléphone."

    const val SESSION_IN_PROGRESS_MESSAGE = "Une session est déjà en cours."

    // Début du blocage (Lot 6, SPEC_ANDROID §15 « Écran 5 — début du blocage »).

    const val BLOCKING_SECTION_TITLE = "Blocage des applications"

    const val BLOCKING_NOW_LABEL = "Maintenant"

    const val BLOCKING_AT_LABEL = "À partir de"

    /** Boutons du sélecteur d'heure de début, hors §15 : un dialogue doit pouvoir être annulé. */
    const val BLOCKING_CONFIRM_LABEL = "Valider"

    const val BLOCKING_DISMISS_LABEL = "Annuler"

    const val BLOCKING_IMMEDIATE_SENTENCE = "Tes applications seront bloquées dès l'activation."

    /**
     * Refus d'antériorité (SPEC_CORE_KMP §8.3, `NOT_BEFORE_TRIGGER`). Ce n'est pas un incident : le
     * texte explique la règle et nomme la sortie, sans couleur d'alerte à l'écran (§15, charte §5).
     * L'écran 6 affiche la **même** phrase pour le même fait — `SummaryTexts` délègue ici.
     */
    const val BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE =
        "L'heure de début du blocage doit être avant ton réveil. " +
            "Pour bloquer tout de suite, choisis « Maintenant »."

    /**
     * Phrase de confirmation d'un début différé (§15) : « Tes applications seront bloquées
     * aujourd'hui, lundi 15 septembre à 22:30 (Europe/Paris). ». Elle continue une phrase déjà
     * commencée, d'où la minuscule initiale — appliquée à la **première lettre seulement**, pour ne
     * pas écraser le nom du fuseau que porte la fin de [WakeScheduleDisplay.sentence].
     */
    fun blockingDeferredSentence(display: WakeScheduleDisplay): String =
        "Tes applications seront bloquées " +
            display.sentence.replaceFirstChar { first -> first.lowercase(Locale.FRANCE) } +
            "."
}
