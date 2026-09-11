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
 */
object PhaseCompletion {
    private val activationRequired = setOf(SessionEffectKindDto.SCHEDULE_ALARM, SessionEffectKindDto.APPLY_BLOCKING)
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
