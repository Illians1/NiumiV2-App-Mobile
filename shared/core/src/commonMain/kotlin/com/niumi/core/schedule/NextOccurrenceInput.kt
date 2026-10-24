package com.niumi.core.schedule

/**
 * Entrée du calcul de la prochaine occurrence d'un [WakePlan] (SPEC_CORE_KMP §8.4). [zoneId] est le
 * fuseau IANA courant, [nowEpochMillis] l'horloge reçue explicitement (§14).
 */
public data class NextOccurrenceInput(
    val plan: WakePlan,
    val zoneId: String,
    val nowEpochMillis: Long,
)
