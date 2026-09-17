package com.niumi.feature.session.wake

import com.niumi.feature.session.ui.WakeScheduleDisplay
import com.niumi.feature.session.ui.WakeScheduleFormatter

/** Heure par défaut du cadran tant qu'aucune heure n'a jamais été confirmée (décision utilisateur). */
const val DEFAULT_LOCAL_TIME_ISO = "07:00"

/**
 * Heure proposée par le sélecteur de début de blocage quand aucune n'a jamais été confirmée
 * (SPEC_ANDROID §15, Lot 6). N'engage rien : tant qu'elle n'est pas confirmée, le mode reste
 * « Maintenant ».
 */
const val DEFAULT_BLOCKING_START_LOCAL_TIME_ISO = "22:00"

/**
 * État de l'écran 5 (SPEC_ANDROID §15, écran 5 ; SPEC_CORE_KMP §8.1, §8.3). [display] n'est non nul
 * que si `computeWakeSchedule` a produit un horaire `VALID` pour [localTimeIso] : sinon [message]
 * porte l'explication et [canContinue] reste faux (§15, ne jamais afficher un faux état).
 *
 * Le début du blocage suit le même principe, avec son propre couple
 * [blockingDisplay]/[blockingMessage] : un refus d'antériorité (§8.3, `NOT_BEFORE_TRIGGER`) n'est
 * pas un problème d'heure de réveil et ne doit pas en effacer l'affichage. [blockingLocalTimeIso]
 * nul décrit un blocage immédiat, seul état dans lequel aucun instant de début n'existe.
 */
data class WakeTimeUiState(
    val localTimeIso: String = DEFAULT_LOCAL_TIME_ISO,
    val display: WakeScheduleDisplay? = null,
    val blockingLocalTimeIso: String? = null,
    val blockingDisplay: WakeScheduleDisplay? = null,
    val blockingMessage: String? = null,
    val use24Hour: Boolean = true,
    val message: String? = null,
    val isSessionInProgress: Boolean = false,
    val isLoading: Boolean = true,
) {
    val isBlockingImmediate: Boolean get() = blockingLocalTimeIso == null

    /**
     * Heure affichée par la ligne qui ouvre le sélecteur de début : l'heure **saisie**, dans la
     * convention du système, comme le cadran du réveil. `null` pour un blocage immédiat.
     *
     * **Constat D1 de l'étape 24, mesuré sur appareil le 2026-09-17.** La ligne montrait l'instant
     * obtenu quand le choix était valide et la saisie ISO quand il était refusé — d'où « 15:00 »
     * sous un réveil « 7:00 AM » sur un téléphone en 12 h. Le rôle était incohérent : cette ligne
     * est un champ de saisie, et le sélecteur qu'elle ouvre se rouvre toujours sur la saisie. La
     * règle « afficher l'instant obtenu, jamais l'heure saisie » (§15) porte sur [blockingSentence]
     * et sur les écrans 6 et 7, pas ici.
     */
    val blockingTimeLabel: String?
        get() = blockingLocalTimeIso?.let { iso -> WakeScheduleFormatter.formatLocalTime(iso, use24Hour) }

    /**
     * Phrase de confirmation du blocage (§15), `null` quand il n'y a rien à confirmer.
     *
     * **Défaut mesuré sur appareil le 2026-09-17.** Un début différé refusé rendait `null` comme un
     * blocage immédiat, et l'écran annonçait « Tes applications seront bloquées dès l'activation. »
     * juste au-dessus du message de refus, alors que « À partir de » restait sélectionné : deux
     * phrases contradictoires, dont l'une promettait un blocage que l'activation n'aurait pas
     * appliqué. C'est le faux état de fiabilité interdit par §15. La phrase se décide donc sur le
     * **mode choisi**, pas sur la présence d'un affichage.
     */
    val blockingSentence: String?
        get() =
            if (isBlockingImmediate) {
                WakeTimeTexts.BLOCKING_IMMEDIATE_SENTENCE
            } else {
                blockingDisplay?.let(WakeTimeTexts::blockingDeferredSentence)
            }

    /**
     * Un blocage immédiat est toujours valide (§8.3) ; un blocage différé exige un instant obtenu
     * et aucun message de refus — « Continuer » reste inactif sinon (§15, écran 5).
     */
    val canContinue: Boolean
        get() =
            display != null &&
                !isSessionInProgress &&
                (isBlockingImmediate || (blockingDisplay != null && blockingMessage == null))
}
