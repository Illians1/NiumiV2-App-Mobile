package com.niumi.system.audio

import android.media.AudioAttributes
import com.niumi.system.common.Clock
import com.niumi.system.common.OperationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Orchestre lecteur, focus audio et vibration derrière [AlarmAudioEngine] (SPEC_ANDROID §10.2).
 * Ne construit aucune classe Android elle-même : la traduction vers `MediaPlayer` et
 * `AudioAttributes` vit dans les implémentations Android de [AlarmPlayerFactory],
 * [AudioFocusController] et [VibrationController], injectées ici et remplaçables par des
 * fakes en test.
 *
 * **Montée progressive (Lot 7).** Le lecteur naît à l'amplitude du temps déjà écoulé depuis
 * `VolumeRamp.startedAtEpochMillis`, puis un job sur [rampScope] la réajuste toutes les
 * `VolumeRampPolicy.TICK_MS` d'après [clock], jusqu'à la durée. Tout l'état est gardé par [lock] :
 * `start` vient du service, `stop` de l'exécuteur de libération, les pas de la rampe de [rampScope],
 * et un pas ne doit jamais toucher un lecteur déjà libéré.
 */
class DefaultAlarmAudioEngine(
    private val playerFactory: AlarmPlayerFactory,
    private val focusController: AudioFocusController,
    private val vibrationController: VibrationController,
    private val clock: Clock,
    private val rampScope: CoroutineScope,
) : AlarmAudioEngine {
    private val lock = Any()
    private var player: AlarmPlayer? = null
    private var rampJob: Job? = null

    override val isPlaying: Boolean
        get() = synchronized(lock) { player != null }

    override fun start(sound: AlarmSound): OperationResult =
        synchronized(lock) {
            if (player != null) return OperationResult.AlreadySatisfied

            val ramp = sound.volumeRamp
            val elapsedMs = ramp?.let { clock.nowEpochMillis() - it.startedAtEpochMillis }
            val configuration =
                AlarmAudioConfiguration(
                    usage = AudioAttributes.USAGE_ALARM,
                    contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION,
                    looping = true,
                    vibrationEnabled = sound.vibrationEnabled,
                    initialAmplitude =
                        if (ramp == null || elapsedMs == null) {
                            1f
                        } else {
                            VolumeRampPolicy.amplitudeAt(elapsedMs, ramp.durationMs)
                        },
                )

            // playerFactory est une interface injectée : sa mise en oeuvre concrète (MediaPlayer)
            // peut lancer IllegalStateException, IllegalArgumentException ou une autre exception
            // d'exécution selon l'état système. Un moteur audio ne doit jamais faire planter
            // l'appelant (SPEC_ANDROID §18 : une erreur audio garde l'activité visible plutôt que
            // de terminer la session) : la capture large est le contrat voulu, pas un oubli.
            @Suppress("TooGenericExceptionCaught")
            try {
                focusController.request(configuration)
                val created = playerFactory.create(sound.ringtoneKey, configuration)
                player = created
                if (sound.vibrationEnabled) vibrationController.startRepeating()
                if (ramp != null && elapsedMs != null && !VolumeRampPolicy.isComplete(elapsedMs, ramp.durationMs)) {
                    rampJob = launchRamp(created, ramp)
                }
                OperationResult.Success
            } catch (error: RuntimeException) {
                player = null
                OperationResult.Failure("ANDROID_AUDIO_START_FAILED", error)
            }
        }

    override fun stop(): OperationResult =
        synchronized(lock) {
            val current = player ?: return OperationResult.AlreadySatisfied
            rampJob?.cancel()
            rampJob = null
            current.release()
            focusController.release()
            vibrationController.stop()
            player = null
            OperationResult.Success
        }

    private fun launchRamp(
        target: AlarmPlayer,
        ramp: VolumeRamp,
    ): Job =
        rampScope.launch {
            do {
                delay(VolumeRampPolicy.TICK_MS)
                val elapsedMs = clock.nowEpochMillis() - ramp.startedAtEpochMillis
                val applied = adjust(target, VolumeRampPolicy.amplitudeAt(elapsedMs, ramp.durationMs))
            } while (applied && !VolumeRampPolicy.isComplete(elapsedMs, ramp.durationMs))
        }

    /**
     * Un pas de rampe. Faux si le lecteur a changé ou si `setVolume` a échoué : la rampe s'arrête
     * alors, sans exception — une exception non rattrapée dans [rampScope] ferait planter le
     * processus, donc tomber le réveil entier pour un simple réglage de volume.
     */
    private fun adjust(
        target: AlarmPlayer,
        amplitude: Float,
    ): Boolean =
        synchronized(lock) {
            if (player !== target) return false
            // L'exception est volontairement absorbée : elle arrête la rampe, rien de plus. Le son
            // continue au dernier volume appliqué, et le moteur n'a pas de journal à qui la confier.
            @Suppress("TooGenericExceptionCaught", "SwallowedException")
            try {
                target.setVolume(amplitude)
                true
            } catch (error: RuntimeException) {
                false
            }
        }
}
