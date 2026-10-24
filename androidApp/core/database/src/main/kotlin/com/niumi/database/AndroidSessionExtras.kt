package com.niumi.database

/**
 * Données propres à Android d'une session, absentes de `SessionSnapshotDto` (SPEC_CORE_KMP §7.1)
 * mais présentes dans `AlarmSessionEntity` (SPEC_ANDROID §7.2) : `ringtoneKey`, `vibrationEnabled`
 * et `volumeRampSeconds` choisis à l'activation, `blockedPackages` sélectionnées par l'utilisateur (« Interfaces
 * transverses » du plan MVP). Aucune liste de Niumi Spots depuis l'étape 43 : la vérification d'un
 * scan lit l'ensemble courant de `PairedBoxStore` (SPEC_CORE_KMP §2, décisions 11 et 16).
 *
 * `volumeRampSeconds` (Lot 7) : durée de la montée progressive, `null` pour un volume constant.
 * Sans valeur par défaut, pour qu'aucune construction ne l'omette sans le voir.
 */
data class AndroidSessionExtras(
    val ringtoneKey: String,
    val vibrationEnabled: Boolean,
    val volumeRampSeconds: Int?,
    val blockedPackages: List<BlockedPackage>,
)
