package com.niumi.system.audio

import com.niumi.system.common.OperationResult

/**
 * Pré-écoute d'une sonnerie sur l'écran 14 (SPEC_ANDROID §10.2, §15, Lot 7). Jamais
 * [AlarmAudioEngine], jamais le service : le moteur de réveil est un singleton dont `start` est
 * idempotent, et une pré-écoute qui le traverserait rendrait un vrai déclenchement sans effet.
 */
interface RingtonePreviewPlayer {
    /** Remplace une pré-écoute en cours. */
    fun play(ringtoneKey: String): OperationResult

    /** Idempotent. */
    fun stop()

    val isPlaying: Boolean
}

/**
 * Fabrique d'un lecteur à lecture unique : [onCompletion] est rappelé quand le fichier a été joué
 * jusqu'au bout. Peut lancer, comme [AlarmPlayerFactory].
 */
fun interface PreviewPlayerFactory {
    fun create(
        ringtoneKey: String,
        configuration: AlarmAudioConfiguration,
        onCompletion: () -> Unit,
    ): AlarmPlayer
}
