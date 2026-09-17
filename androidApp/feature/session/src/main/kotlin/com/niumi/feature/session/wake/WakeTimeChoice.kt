package com.niumi.feature.session.wake

/**
 * Les deux choix de l'écran 5, transmis à l'écran 6 (SPEC_ANDROID §15 ; SPEC_CORE_KMP §8.1, §8.3) :
 * des heures locales **choisies**, jamais les instants qui en dérivent — ceux-ci sont recalculés
 * avant l'activation, et transporter un horaire déjà calculé rendrait structurellement possible
 * d'armer une session sur un instant périmé.
 *
 * [blockingLocalTimeIso] nul décrit un blocage immédiat, le défaut du produit.
 */
data class WakeTimeChoice(
    val localTimeIso: String,
    val blockingLocalTimeIso: String? = null,
)
