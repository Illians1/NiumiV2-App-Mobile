package com.niumi.system.session

import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionEventKindDto

/**
 * Effets requis d'une phase (SPEC_CORE_KMP §6, dernier alinéa) : conditionnent l'envoi de
 * l'événement `_SUCCEEDED` correspondant. Décision validée le 2026-09-10 : `STOP_RINGING` est
 * requis pour `RELEASE_SUCCEEDED` — SPEC_CORE_KMP §6 et SPEC_ANDROID §11.3 se contredisaient ;
 * déclarer une session terminée pendant que le réveil sonne encore serait le pire résultat
 * possible pour un produit de réveil, et l'outbox existe pour rejouer l'arrêt du son.
 * SPEC_ANDROID §11.3 est corrigée en conséquence dans le même changement.
 *
 * `CANCEL_BLOCKING_START` reste absent de [releaseRequired] : SPEC_CORE_KMP §6 le range parmi les
 * effets best-effort, une alarme de début laissée programmée après un état final étant absorbée par
 * `BlockingStartHandler`, dont le moteur refuse l'événement faute de session `ARMED` en attente.
 */
object PhaseCompletion {
    /**
     * Les trois effets dont dépend `ACTIVATION_SUCCEEDED`, dont **deux seulement sont produits à la
     * fois** : `APPLY_BLOCKING` pour un blocage immédiat, `SCHEDULE_BLOCKING_START` pour un blocage
     * différé (SPEC_CORE_KMP §6, Lot 6). Aucune intersection avec les kinds produits n'est
     * nécessaire : [EffectOutcomes.succeeded] est un `all {}` sur les outcomes de ce kind, donc
     * vacuement vrai pour un kind que la décision n'a pas produit. Une activation immédiate ignore
     * ainsi `SCHEDULE_BLOCKING_START`, et ses effets restent exactement ceux du contrat 1.2 —
     * ordinaux et `effectId` compris.
     */
    private val activationRequired =
        setOf(
            SessionEffectKindDto.SCHEDULE_ALARM,
            SessionEffectKindDto.APPLY_BLOCKING,
            SessionEffectKindDto.SCHEDULE_BLOCKING_START,
        )
    private val releaseRequired =
        setOf(
            SessionEffectKindDto.CANCEL_ALARM,
            SessionEffectKindDto.STOP_RINGING,
            SessionEffectKindDto.REMOVE_BLOCKING,
        )

    fun requiredKindsFor(eventKind: SessionEventKindDto): Set<SessionEffectKindDto> =
        when (eventKind) {
            SessionEventKindDto.ACTIVATION_REQUESTED -> activationRequired
            SessionEventKindDto.VALID_NFC_SCANNED -> releaseRequired
            else -> emptySet()
        }

    fun isSatisfied(
        eventKind: SessionEventKindDto,
        outcomes: EffectOutcomes,
    ): Boolean = requiredKindsFor(eventKind).all { outcomes.succeeded(it) }

    fun firstFailureCode(
        eventKind: SessionEventKindDto,
        outcomes: EffectOutcomes,
    ): String? = requiredKindsFor(eventKind).firstNotNullOfOrNull { outcomes.failureCodeOf(it) }
}
