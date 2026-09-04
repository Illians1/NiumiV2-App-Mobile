package com.niumi.system.audio

/**
 * Description pure de la configuration audio d'alarme (SPEC_ANDROID §10.2) : ni `MediaPlayer`
 * ni `AudioAttributes` ne sont construits ici, seulement les valeurs qui les paramètrent.
 * `usage` et `contentType` sont les constantes entières `AudioAttributes.USAGE_ALARM` et
 * `AudioAttributes.CONTENT_TYPE_SONIFICATION`, inlinées à la compilation : lisibles dans un
 * test JVM même si `AudioAttributes` lui-même y est un stub.
 */
data class AlarmAudioConfiguration(
    val usage: Int,
    val contentType: Int,
    val looping: Boolean,
    val vibrationEnabled: Boolean,
)
