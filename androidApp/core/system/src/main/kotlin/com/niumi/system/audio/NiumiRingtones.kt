package com.niumi.system.audio

/**
 * Clés de sonnerie connues du produit. `ArmSessionUseCase` (étape 14) fige
 * [DEFAULT_KEY] dans `AndroidSessionExtras.ringtoneKey` au moment de l'activation : seule cette
 * clé se résout aujourd'hui vers une ressource concrète, dans `RingingModule` (`:feature:ringing`,
 * SPEC_ANDROID §6 — `:core:system` ne peut pas référencer le `R` d'un module `feature`).
 *
 * `AlarmRingingService` ne relit pas encore `extras.ringtoneKey` : il démarre toujours cette même
 * clé en dur, un réglage utilisateur du choix de la sonnerie n'existant pas dans le MVP. Sans
 * effet tant qu'une seule sonnerie existe ; à corriger le jour où une deuxième sera proposée.
 */
object NiumiRingtones {
    const val DEFAULT_KEY = "niumi_alarm"
}
