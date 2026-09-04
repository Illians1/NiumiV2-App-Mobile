package com.niumi.system.audio

/** Demande et libère le focus audio avec la même configuration que le lecteur d'alarme. */
interface AudioFocusController {
    fun request(configuration: AlarmAudioConfiguration): Boolean

    fun release()
}
