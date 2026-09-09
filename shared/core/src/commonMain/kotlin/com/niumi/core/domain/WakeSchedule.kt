package com.niumi.core.domain

/**
 * Programmation du réveil (SPEC_CORE_KMP §7.2). Les trois premiers champs conservent l'intention
 * saisie par l'utilisateur ; [triggerAtEpochMillis] est l'instant contractuel utilisé par les deux
 * plateformes et ne se déplace pas à un changement de fuseau (SPEC_CORE_KMP §2, décision 9).
 */
public data class WakeSchedule(
    val localDateIso: String,
    val localTimeIso: String,
    val zoneIdAtActivation: String,
    val triggerAtEpochMillis: Long,
)
