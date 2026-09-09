package com.niumi.core.domain

/**
 * Santé globale de la session (SPEC_CORE_KMP §7.3). Un incident `WARNING` la laisse inchangée ;
 * un incident `DEGRADED` ou `CRITICAL` la fait passer à `DEGRADED` sans retour automatique à
 * `HEALTHY` tant que la session est active.
 */
public enum class SessionHealth {
    HEALTHY,
    DEGRADED,
}
