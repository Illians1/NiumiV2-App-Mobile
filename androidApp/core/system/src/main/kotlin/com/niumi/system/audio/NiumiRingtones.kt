package com.niumi.system.audio

/** Une sonnerie du catalogue : sa clé, figée dans la session, et son libellé affiché. */
data class Ringtone(
    val key: String,
    val label: String,
)

/**
 * Catalogue des sonneries empaquetées (SPEC_ANDROID §10.2, Lot 7). Seule liste du produit : une
 * clé absente d'ici n'a pas de ressource. La résolution vers `R.raw` vit dans `RingtoneResources`
 * (`:feature:ringing`, SPEC_ANDROID §6 — `:core:system` ne peut pas référencer le `R` d'un module
 * `feature`).
 */
object NiumiRingtones {
    /** « Piano », décision du 2026-09-30 (plan maître, phase J, décision 2 révisée). */
    const val DEFAULT_KEY = "niumi_piano"

    /**
     * Sonnerie unique du MVP, retirée de l'APK au Lot 7. Une session armée avant la mise à jour la
     * porte encore : `MIGRATION_3_4` et `DirectBootMapper` la réécrivent en [DEFAULT_KEY].
     */
    const val LEGACY_KEY = "niumi_alarm"

    /** Ordre d'affichage de l'écran 14. */
    val ALL: List<Ringtone> =
        listOf(
            Ringtone("niumi_bell", "Cloche"),
            Ringtone("niumi_energique", "Énergique"),
            Ringtone("niumi_oiseaux", "Oiseaux"),
            Ringtone(DEFAULT_KEY, "Piano"),
        )

    fun byKey(key: String): Ringtone? = ALL.firstOrNull { it.key == key }
}
