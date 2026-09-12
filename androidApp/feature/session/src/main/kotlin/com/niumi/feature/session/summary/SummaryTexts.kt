package com.niumi.feature.session.summary

import com.niumi.core.diagnostics.ActivationReasonCode

/**
 * Textes de l'écran 6 (SPEC_ANDROID §15, écran 6). Ne traduit que **quatre** codes de refus : les
 * quatorze messages détaillés du diagnostic restent la propriété de l'écran 2, seul endroit qui
 * porte l'action de remédiation correspondante (§13, une seule action principale à la fois).
 */
object SummaryTexts {
    const val TITLE = "Ton engagement"

    const val ACTIVATE_BUTTON_LABEL = "Activer ma session"

    const val CHANGE_TIME_LABEL = "Choisir une autre heure"

    const val COMMITMENT_REMINDER = "Seul le scan du boîtier terminera la session."

    const val BLOCKED_APPS_TITLE = "Applications bloquées"

    const val BOX_TITLE = "Boîtier associé"

    /**
     * Explication de l'écart entre l'heure saisie et l'heure programmée lors d'un trou d'heure
     * d'été (SPEC_CORE_KMP §8.1, point 3). Affichée seulement dans ce cas : sans elle, l'écart
     * passerait pour un défaut de l'application.
     */
    fun daylightSavingShift(
        requestedLocalTime: String,
        actualLocalTime: String,
    ): String =
        "L'heure $requestedLocalTime n'existe pas cette nuit-là : le changement d'heure fait " +
            "sauter l'horloge. Ton réveil sonnera dès $actualLocalTime."

    const val SESSION_IN_PROGRESS_MESSAGE = "Une session est déjà en cours."

    const val INVALID_SCHEDULE_MESSAGE = "L'heure choisie n'est plus valide. Choisis-en une autre."

    const val REJECTED_MESSAGE =
        "Niumi n'a pas pu créer cette session. Vérifie qu'aucune session n'est déjà en cours."

    const val DUPLICATE_MESSAGE = "Cette activation a déjà été enregistrée. Vérifie l'état de ta session."

    /** Le `failureCode` est nommé : il est la seule information exploitable par le support (§16, §18). */
    fun activationFailed(failureCode: String?): String =
        if (failureCode == null) {
            "L'activation a échoué. Réessaie."
        } else {
            "L'activation a échoué ($failureCode). Réessaie."
        }

    /**
     * Un refus de la politique commune. Les causes qui appartiennent à cet écran ou au précédent
     * sont nommées ; les contrôles d'appareil renvoient au diagnostic, qui seul sait les corriger.
     */
    fun blockingReason(code: String): String =
        when (code) {
            ActivationReasonCode.TRIGGER_NOT_IN_FUTURE -> {
                "L'heure choisie est déjà passée. Choisis une heure future."
            }

            ActivationReasonCode.NO_PAIRED_BOX -> {
                "Aucun boîtier n'est associé. Associe ton boîtier avant d'activer ta session."
            }

            ActivationReasonCode.INVALID_APP_SELECTION -> {
                "Ta sélection d'applications n'est pas valide. Choisis entre 1 et 50 applications."
            }

            else -> {
                "Ton appareil n'est pas prêt. Reviens au diagnostic pour corriger ce qui bloque."
            }
        }
}
