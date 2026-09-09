package com.niumi.core.domain

/**
 * Effet ordonné produit par une décision (SPEC_CORE_KMP §6). [effectId] est déterministe :
 * `"$sessionId:$revision:$kind:$ordinal"` (voir [EffectIdFactory]), ce qui rend l'outbox native
 * idempotente à la reprise.
 */
public data class SessionEffect(
    val effectId: String,
    val kind: SessionEffectKind,
    val sessionId: String,
    val revision: Long,
    val payload: SessionEffectPayload?,
)
