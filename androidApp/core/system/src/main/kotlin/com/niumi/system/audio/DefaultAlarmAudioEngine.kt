package com.niumi.system.audio

import android.media.AudioAttributes
import com.niumi.system.common.OperationResult

/**
 * Orchestre lecteur, focus audio et vibration derrière [AlarmAudioEngine] (SPEC_ANDROID §10.2).
 * Ne construit aucune classe Android elle-même : la traduction vers `MediaPlayer` et
 * `AudioAttributes` vit dans les implémentations Android de [AlarmPlayerFactory],
 * [AudioFocusController] et [VibrationController], injectées ici et remplaçables par des
 * fakes en test.
 */
class DefaultAlarmAudioEngine(
    private val playerFactory: AlarmPlayerFactory,
    private val focusController: AudioFocusController,
    private val vibrationController: VibrationController,
) : AlarmAudioEngine {
    private var player: AlarmPlayer? = null

    override val isPlaying: Boolean
        get() = player != null

    override fun start(
        ringtoneKey: String,
        vibrationEnabled: Boolean,
    ): OperationResult {
        if (player != null) return OperationResult.AlreadySatisfied

        val configuration =
            AlarmAudioConfiguration(
                usage = AudioAttributes.USAGE_ALARM,
                contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION,
                looping = true,
                vibrationEnabled = vibrationEnabled,
            )

        // playerFactory est une interface injectée : sa mise en oeuvre concrète (MediaPlayer)
        // peut lancer IllegalStateException, IllegalArgumentException ou une autre exception
        // d'exécution selon l'état système. Un moteur audio ne doit jamais faire planter
        // l'appelant (SPEC_ANDROID §18 : une erreur audio garde l'activité visible plutôt que
        // de terminer la session) : la capture large est le contrat voulu, pas un oubli.
        @Suppress("TooGenericExceptionCaught")
        return try {
            focusController.request(configuration)
            player = playerFactory.create(ringtoneKey, configuration)
            if (vibrationEnabled) vibrationController.startRepeating()
            OperationResult.Success
        } catch (error: RuntimeException) {
            player = null
            OperationResult.Failure("ANDROID_AUDIO_START_FAILED", error)
        }
    }

    override fun stop(): OperationResult {
        val current = player ?: return OperationResult.AlreadySatisfied
        current.release()
        focusController.release()
        vibrationController.stop()
        player = null
        return OperationResult.Success
    }
}
