package com.niumi.system.audio

/**
 * Résout une clé de sonnerie (ex. `"niumi_alarm"`) vers une ressource `R.raw` concrète.
 * `:core:system` ne peut pas référencer le `R` de `:feature:ringing`, propriétaire du fichier
 * audio empaqueté (SPEC_ANDROID §10.2) : implémenté dans ce module downstream.
 */
fun interface RingtoneResourceResolver {
    fun resourceId(ringtoneKey: String): Int?
}
