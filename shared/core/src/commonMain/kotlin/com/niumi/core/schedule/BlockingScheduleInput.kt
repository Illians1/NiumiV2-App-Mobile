package com.niumi.core.schedule

/**
 * Entrée du calcul du début du blocage (SPEC_CORE_KMP §8.3). [localTimeIso] nul décrit un blocage
 * immédiat. [zoneId] et [nowEpochMillis] sont ceux du calcul du réveil : une session n'a qu'un
 * fuseau d'activation. [triggerAtEpochMillis] est l'instant du réveil déjà obtenu, dont le début du
 * blocage doit être strictement antérieur.
 */
public data class BlockingScheduleInput(
    val localTimeIso: String?,
    val zoneId: String,
    val nowEpochMillis: Long,
    val triggerAtEpochMillis: Long,
)
