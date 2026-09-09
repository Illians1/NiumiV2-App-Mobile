package com.niumi.core.domain

/**
 * Numérote les effets d'une décision et calcule leur [SessionEffect.effectId] via
 * [EffectIdFactory]. Un seul point du code construit des [SessionEffect] : garantit que l'ordinal
 * suit l'ordre de la table des effets de SPEC_CORE_KMP §6.
 */
internal class SessionEffectBuilder(
    private val sessionId: String,
    private val revision: Long,
) {
    private val effects = mutableListOf<SessionEffect>()

    internal fun add(
        kind: SessionEffectKind,
        payload: SessionEffectPayload? = null,
    ): SessionEffectBuilder {
        val ordinal = effects.size
        effects +=
            SessionEffect(
                effectId = EffectIdFactory.effectIdOf(sessionId, revision, kind, ordinal),
                kind = kind,
                sessionId = sessionId,
                revision = revision,
                payload = payload,
            )
        return this
    }

    internal fun build(): List<SessionEffect> = effects.toList()
}
