package com.niumi.database

/**
 * Données propres à Android d'une session, absentes de `SessionSnapshotDto` (SPEC_CORE_KMP §7.1)
 * mais présentes dans `AlarmSessionEntity`/`session_box` (SPEC_ANDROID §7.2) : `boxes` copiée
 * depuis `PairedBoxStore` à `ACTIVATION_REQUESTED` et figée pour la durée de la session (Lot 8,
 * remplace `boxId`/`boxTokenSha256Hex`), `ringtoneKey`, `vibrationEnabled` et `volumeRampSeconds`
 * choisis à l'activation, `blockedPackages` sélectionnées par l'utilisateur (« Interfaces
 * transverses » du plan MVP).
 *
 * `volumeRampSeconds` (Lot 7) : durée de la montée progressive, `null` pour un volume constant.
 * Sans valeur par défaut, pour qu'aucune construction ne l'omette sans le voir.
 */
data class AndroidSessionExtras(
    val boxes: List<SessionBox>,
    val ringtoneKey: String,
    val vibrationEnabled: Boolean,
    val volumeRampSeconds: Int?,
    val blockedPackages: List<BlockedPackage>,
)
