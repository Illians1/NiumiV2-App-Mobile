package com.niumi.core.domain

/**
 * Gravité d'un [SessionIncident] (SPEC_CORE_KMP §7.3). `CRITICAL` exige en plus une présentation
 * explicite dans un diagnostic visible par l'utilisateur, `DEGRADED` peut rester consigné sans
 * interrompre le parcours.
 */
public enum class IncidentSeverity {
    WARNING,
    DEGRADED,
    CRITICAL,
}
