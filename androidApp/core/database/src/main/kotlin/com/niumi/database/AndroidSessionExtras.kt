package com.niumi.database

/**
 * Données propres à Android d'une session, absentes de `SessionSnapshotDto` (SPEC_CORE_KMP §7.1)
 * mais présentes dans `AlarmSessionEntity` (SPEC_ANDROID §7.2) : `boxId` et `boxTokenSha256Hex`
 * copiés depuis `PairedBoxEntity` à `ACTIVATION_REQUESTED` et figés pour la durée de la session,
 * `ringtoneKey` et `vibrationEnabled` choisis à l'activation, `blockedPackages` sélectionnées par
 * l'utilisateur (« Interfaces transverses » du plan MVP).
 */
data class AndroidSessionExtras(
    val boxId: String,
    val boxTokenSha256Hex: String,
    val ringtoneKey: String,
    val vibrationEnabled: Boolean,
    val blockedPackages: List<BlockedPackage>,
)
