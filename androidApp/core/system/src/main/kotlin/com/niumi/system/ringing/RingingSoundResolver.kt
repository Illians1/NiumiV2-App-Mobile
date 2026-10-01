package com.niumi.system.ringing

import com.niumi.system.audio.AlarmSound
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.VolumeRamp
import com.niumi.system.session.LoadResult

private const val MILLIS_PER_SECOND = 1_000L

/** Ce que [RingingSoundResolver] fait sonner ; [fallback] vrai si la clé de la session a été remplacée. */
data class ResolvedAlarmSound(
    val sound: AlarmSound,
    val fallback: Boolean,
)

/**
 * Seul à décider de ce qui sonne (SPEC_ANDROID §10.2, Lot 7). Fonction pure, hors du service,
 * comme [RingingServiceRecovery] : le service n'est pas testable en JVM.
 *
 * - session présente, même `sessionId` : sa sonnerie, sa vibration et sa montée, la montée partant
 *   de `ringingAtEpochMillis` pour reprendre là où elle en était après une mort du processus ;
 *   une clé hors catalogue devient la sonnerie par défaut, avec `fallback = true` ;
 * - session absente, illisible ou autre : sonnerie par défaut, vibration, **volume constant** —
 *   sans savoir ce qui a été choisi, le réveil ne démarre jamais bas, et ne se tait jamais.
 */
object RingingSoundResolver {
    private val SAFE_DEFAULT = AlarmSound(NiumiRingtones.DEFAULT_KEY, vibrationEnabled = true, volumeRamp = null)

    fun resolve(
        loaded: LoadResult,
        sessionId: String,
        nowEpochMillis: Long,
    ): ResolvedAlarmSound {
        val present =
            (loaded as? LoadResult.Present)?.takeIf { it.snapshot.sessionId == sessionId }
                ?: return ResolvedAlarmSound(SAFE_DEFAULT, fallback = false)
        val extras = present.extras
        val known = NiumiRingtones.byKey(extras.ringtoneKey) != null
        return ResolvedAlarmSound(
            sound =
                AlarmSound(
                    ringtoneKey = if (known) extras.ringtoneKey else NiumiRingtones.DEFAULT_KEY,
                    vibrationEnabled = extras.vibrationEnabled,
                    volumeRamp =
                        extras.volumeRampSeconds?.let { seconds ->
                            VolumeRamp(
                                durationMs = seconds * MILLIS_PER_SECOND,
                                startedAtEpochMillis = present.snapshot.ringingAtEpochMillis ?: nowEpochMillis,
                            )
                        },
                ),
            fallback = !known,
        )
    }
}
