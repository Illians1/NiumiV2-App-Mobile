package com.niumi.core.schedule

/**
 * Planning de réveil récurrent (SPEC_CORE_KMP §8.4, contrat 1.5). [enabledIsoDays] porte les jours
 * de **réveil** cochés, en numérotation ISO (1 = lundi … 7 = dimanche) ; [wakeTimeIso] et
 * [bedtimeIso] sont les intentions locales (ex. `"07:00"`, `"22:30"`). Le planning est calculé ici
 * et stocké par chaque plateforme : l'interrupteur général et le dépôt restent natifs.
 */
public data class WakePlan(
    val enabledIsoDays: Set<Int>,
    val wakeTimeIso: String,
    val bedtimeIso: String,
)
