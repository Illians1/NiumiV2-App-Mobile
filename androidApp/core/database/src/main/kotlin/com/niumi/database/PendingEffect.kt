package com.niumi.database

import com.niumi.core.interop.SessionEffectKindDto

/**
 * État d'exécution d'un effet dans l'outbox native (SPEC_CORE_KMP §6.1). `SATISFIED` couvre le
 * cas d'un effet requis dont la précondition a déjà disparu (§6, dernier alinéa) : il n'est ni un
 * succès d'exécution ni un échec, mais un état natif cible déjà atteint.
 */
enum class EffectStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    SATISFIED,
}

/**
 * Effet persisté dans l'outbox (SPEC_ANDROID §7.2 : `SessionEffectOutboxEntity`). `ordinal` est
 * l'index de l'effet dans la décision qui l'a produit (`SessionDecisionDto.effects` — le DTO ne le
 * porte pas lui-même, voir `SessionEffectMapper`), utilisé pour reconstituer `effectId` et pour
 * l'ordre de rejeu. `payloadJson` est `null` pour un effet entièrement reconstructible depuis le
 * snapshot.
 */
data class PendingEffect(
    val effectId: String,
    val sessionId: String,
    val revision: Long,
    val kind: SessionEffectKindDto,
    val ordinal: Int,
    val payloadJson: String?,
    val status: EffectStatus,
    val lastError: String?,
)
