package com.niumi.system.audio

import com.niumi.system.common.OperationResult
import kotlinx.coroutines.flow.StateFlow

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

    /**
     * Clé en cours de lecture, ou `null`. Contrairement à [isPlaying], une simple consultation
     * ponctuelle, ce flux notifie aussi la fin naturelle du fichier : sans lui, l'écran 14
     * garderait l'icône « arrêt » affichée après la fin d'une pré-écoute sans boucle (étape 27).
     */
    val playingKey: StateFlow<String?>
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
