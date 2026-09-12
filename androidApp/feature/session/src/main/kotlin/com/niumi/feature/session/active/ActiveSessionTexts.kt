package com.niumi.feature.session.active

import com.niumi.core.interop.SessionStateDto

/** Textes de l'écran 7 (SPEC_ANDROID §15, écran 7), version minimale de l'étape 14. */
object ActiveSessionTexts {
    const val TITLE = "Ta session est active"

    const val NO_SESSION = "Aucune session active."

    const val CURRENT_ZONE_TITLE = "Dans ton fuseau actuel"

    const val COMMITMENT_REMINDER = "Seul le scan du boîtier terminera la session."

    /**
     * Un état métier nommé en clair. `RINGING` et les trois états d'attente de scan ont leur propre
     * écran (§10.4) ; ils ne sont listés ici que pour n'afficher jamais un état inconnu.
     */
    fun stateLabel(state: SessionStateDto): String =
        when (state) {
            SessionStateDto.PREPARING -> "Activation en cours"
            SessionStateDto.ARMED -> "Réveil programmé"
            SessionStateDto.RINGING -> "Ton réveil sonne"
            SessionStateDto.AWAITING_NFC -> "En attente du scan de ton boîtier"
            SessionStateDto.TRIGGERED_AWAITING_NFC -> "En attente du scan de ton boîtier"
            SessionStateDto.RELEASING -> "Déblocage en cours"
            SessionStateDto.COMPLETED -> "Session terminée"
            SessionStateDto.CANCELLED -> "Session annulée"
            SessionStateDto.FAILED -> "L'activation a échoué"
        }
}
