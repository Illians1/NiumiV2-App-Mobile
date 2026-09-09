package com.niumi.core.domain

/**
 * Résultat de `SessionEngine.reduce()` (SPEC_CORE_KMP §6). Une décision refusée renvoie le
 * [snapshot] reçu inchangé et [effects] vide ; [violations] est alors non vide et [effects] et
 * [violations] ne sont jamais tous les deux non vides.
 */
public data class SessionDecision(
    val snapshot: SessionSnapshot?,
    val effects: List<SessionEffect>,
    val violations: List<DomainViolation>,
)
