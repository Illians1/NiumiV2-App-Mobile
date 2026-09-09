package com.niumi.core.domain

/**
 * Charge minimale nécessaire à la reprise d'un effet (SPEC_CORE_KMP §6). `null` pour les effets
 * entièrement reconstructibles depuis le snapshot.
 */
public sealed interface SessionEffectPayload

/** Charge de `RECORD_INCIDENT` (SPEC_CORE_KMP §6). */
public data class IncidentEffectPayload(
    val incident: SessionIncident,
) : SessionEffectPayload
