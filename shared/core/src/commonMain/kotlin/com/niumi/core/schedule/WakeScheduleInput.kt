package com.niumi.core.schedule

/**
 * Entrée du calcul de l'heure de réveil (SPEC_CORE_KMP §8.1). [localTimeIso] est l'intention locale
 * saisie par l'utilisateur (ex. `"07:00"`), [zoneId] un identifiant IANA (ex. `"Europe/Paris"`).
 * [nowEpochMillis] est reçue explicitement : aucune horloge globale cachée (§14).
 */
public data class WakeScheduleInput(
    val localTimeIso: String,
    val zoneId: String,
    val nowEpochMillis: Long,
)
