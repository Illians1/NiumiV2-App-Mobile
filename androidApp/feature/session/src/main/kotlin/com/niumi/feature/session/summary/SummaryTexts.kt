package com.niumi.feature.session.summary

import com.niumi.core.diagnostics.ActivationReasonCode
import com.niumi.core.interop.ActivationReasonDto
import com.niumi.feature.session.wake.WakeTimeTexts
import com.niumi.system.readiness.ReadinessMessages
import com.niumi.system.readiness.readinessCheckIdOf

/**
 * Textes de l'écran 6 (SPEC_ANDROID §15, écran 6). Ne traduit que **cinq** codes de refus communs ;
 * un contrôle d'appareil est nommé par son message de §13 ([refusal]), depuis que le diagnostic de
 * la mise en route a laissé place à l'écran 2 des autorisations (étape 36 de la refonte).
 */
object SummaryTexts {
    const val TITLE = "Ton engagement"

    const val ACTIVATE_BUTTON_LABEL = "Activer ma session"

    const val CHANGE_TIME_LABEL = "Choisir une autre heure"

    /** Lot 8 : « d'un de tes boîtiers », la session se termine par n'importe lequel des boîtiers figés. */
    const val COMMITMENT_REMINDER = "Seul le scan d'un de tes boîtiers terminera la session."

    const val BLOCKED_APPS_TITLE = "Applications bloquées"

    const val BOX_TITLE = "Boîtiers associés"

    /** Ligne « Blocage des applications » du Lot 6 (§15, écran 6). */
    const val BLOCKING_TITLE = "Blocage des applications"

    const val BLOCKING_IMMEDIATE_LABEL = "Dès l'activation"

    /** Ligne « Sonnerie » sous « Blocage des applications » (§15, Lot 7) : affichée, pas cliquable. */
    const val RINGTONE_TITLE = "Sonnerie"

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
     * Un refus de la politique commune. Un contrôle d'appareil (`checkId` renseigné) est nommé par
     * son message de §13, qui dit aussi quoi régler : Ne pas déranger et le volume n'ont pas de
     * tuile sur l'écran 2 (décision Q1 du 2026-10-20), cet écran est le seul à les nommer avant
     * l'activation. Les autres causes gardent leur texte commun ([blockingReason]).
     */
    fun refusal(reason: ActivationReasonDto): String =
        readinessCheckIdOf(reason)?.let(ReadinessMessages::forCheck) ?: blockingReason(reason.code)

    /**
     * Les causes qui appartiennent à cet écran ou au précédent sont nommées. Le repli ne renvoie
     * plus au diagnostic, retiré du parcours à l'étape 36 : seul un refus inattendu, sans contrôle
     * identifié, l'atteint.
     */
    fun blockingReason(code: String): String =
        when (code) {
            ActivationReasonCode.TRIGGER_NOT_IN_FUTURE -> {
                "L'heure choisie est déjà passée. Choisis une heure future."
            }

            ActivationReasonCode.NO_PAIRED_BOX -> {
                "Aucun boîtier n'est associé. Associe au moins un boîtier avant d'activer ta session."
            }

            ActivationReasonCode.INVALID_APP_SELECTION -> {
                "Ta sélection d'applications n'est pas valide. Choisis entre 1 et 50 applications."
            }

            // §15 impose la même phrase qu'à l'écran 5 pour le même fait : une seule chaîne existe,
            // celle du choix, et cet écran la relaie plutôt que d'en garder une copie.
            ActivationReasonCode.BLOCKING_START_NOT_BEFORE_TRIGGER -> {
                WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE
            }

            else -> {
                "Ton appareil n’est pas prêt."
            }
        }
}
