package com.niumi.core.domain

/**
 * Incident technique consigné par le coordinateur natif (SPEC_CORE_KMP §7.3). `code` est un code
 * commun de la table §7.3 ou un code préfixé `ANDROID_`/`IOS_` défini par la plateforme.
 */
public data class SessionIncident(
    val code: String,
    val severity: IncidentSeverity,
    val occurredAtEpochMillis: Long,
    val platform: Platform,
)
