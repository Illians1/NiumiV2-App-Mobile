package com.niumi.feature.session.ringtone.fakes

import com.niumi.core.interop.SessionEventDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.RingtonePreviewPlayer
import com.niumi.system.common.OperationResult
import com.niumi.system.session.AlarmSoundUpdateResult
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.ReconcileResult
import com.niumi.system.session.SessionCoordinator
import kotlinx.coroutines.flow.MutableStateFlow

/** Fakes locaux de l'écran 14 : `:core:system/src/test` n'est pas exporté (aucun `testFixtures`). */
class FakeAlarmSoundPreferences(
    var settings: AlarmSoundSettings = AlarmSoundSettings(),
    var writeFails: Boolean = false,
) : AlarmSoundPreferences {
    var written: AlarmSoundSettings? = null
        private set

    override suspend fun read(): AlarmSoundSettings = settings

    override suspend fun write(settings: AlarmSoundSettings) {
        if (writeFails) error("DATASTORE_WRITE_FAILED")
        this.settings = settings
        written = settings
    }
}

class FakeAlarmVolumeSource(
    var volume: Int = 10,
) : AlarmVolumeSource {
    override fun alarmStreamVolume(): Int = volume
}

/**
 * Lecture unique sans boucle, comme [com.niumi.system.audio.DefaultRingtonePreviewPlayer], mais
 * sans `MediaPlayer` : [completeNaturally] simule la fin du fichier pour prouver que l'écran arrête
 * d'afficher l'icône « arrêt » sans action de l'utilisateur.
 */
class FakeRingtonePreviewPlayer(
    var failingKey: String? = null,
) : RingtonePreviewPlayer {
    private val mutablePlayingKey = MutableStateFlow<String?>(null)
    override val playingKey = mutablePlayingKey

    override val isPlaying: Boolean
        get() = mutablePlayingKey.value != null

    var playCalls = 0
        private set

    override fun play(ringtoneKey: String): OperationResult {
        playCalls++
        if (ringtoneKey == failingKey) return OperationResult.Failure("ANDROID_AUDIO_START_FAILED")
        mutablePlayingKey.value = ringtoneKey
        return OperationResult.Success
    }

    override fun stop() {
        mutablePlayingKey.value = null
    }

    fun completeNaturally() {
        mutablePlayingKey.value = null
    }
}

class FakeSessionCoordinator : SessionCoordinator {
    var result: AlarmSoundUpdateResult = AlarmSoundUpdateResult.Updated
    var lastRingtoneKey: String? = null
        private set
    var lastVolumeRampSeconds: Int? = null
        private set
    var updateCalls = 0
        private set

    override suspend fun dispatch(
        event: SessionEventDto,
        extras: AndroidSessionExtras?,
    ): DispatchResult = throw UnsupportedOperationException("Non utilisé par l'écran 14")

    override suspend fun reconcile(reason: ReconcileReason): ReconcileResult =
        throw UnsupportedOperationException("Non utilisé par l'écran 14")

    override suspend fun updateAlarmSound(
        ringtoneKey: String,
        volumeRampSeconds: Int?,
    ): AlarmSoundUpdateResult {
        updateCalls++
        lastRingtoneKey = ringtoneKey
        lastVolumeRampSeconds = volumeRampSeconds
        return result
    }
}

/** Réglages distincts du défaut, pour distinguer « lu de la session » de « lu de la préférence ». */
val SESSION_EXTRAS_SETTINGS = AlarmSoundSettings("niumi_oiseaux", 30)

fun sessionExtras(): AndroidSessionExtras =
    AndroidSessionExtras(
        boxId = "550e8400-e29b-41d4-a716-446655440000",
        boxTokenSha256Hex = "a".repeat(64),
        ringtoneKey = SESSION_EXTRAS_SETTINGS.ringtoneKey,
        vibrationEnabled = true,
        volumeRampSeconds = SESSION_EXTRAS_SETTINGS.volumeRampSeconds,
        blockedPackages = emptyList(),
    )

val PREFERENCE_SETTINGS = AlarmSoundSettings(NiumiRingtones.DEFAULT_KEY, 300)
