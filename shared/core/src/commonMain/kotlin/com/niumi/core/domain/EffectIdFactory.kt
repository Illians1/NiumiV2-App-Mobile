package com.niumi.core.domain

/**
 * Formule déterministe de `effectId` (SPEC_CORE_KMP §6, forme non fixée par la spec — choix
 * documenté dans le plan d'étape 2 puis repris ici). `revision` est la nouvelle révision produite
 * par la décision, `ordinal` l'index de l'effet dans cette décision.
 */
internal object EffectIdFactory {
    internal fun effectIdOf(
        sessionId: String,
        revision: Long,
        kind: SessionEffectKind,
        ordinal: Int,
    ): String = "$sessionId:$revision:$kind:$ordinal"
}
