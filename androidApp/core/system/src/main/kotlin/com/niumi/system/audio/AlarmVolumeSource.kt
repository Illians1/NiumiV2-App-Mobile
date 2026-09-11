package com.niumi.system.audio

import android.content.Context
import android.media.AudioManager

/** Volume du flux d'alarme (SPEC_ANDROID §7.1 `SessionRuntimeStatus`, champ `audioReady`). */
fun interface AlarmVolumeSource {
    fun alarmStreamVolume(): Int
}

class AndroidAlarmVolumeSource(
    private val context: Context,
) : AlarmVolumeSource {
    override fun alarmStreamVolume(): Int =
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .getStreamVolume(AudioManager.STREAM_ALARM)
}
