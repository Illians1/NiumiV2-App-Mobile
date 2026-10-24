package com.niumi.system.audio

import android.media.AudioManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Feuille Sonnerie, pourcentage en direct (révision du 2026-10-24) : seule une annonce qui peut
 * concerner le flux d'alarme fait relire le volume.
 */
class AlarmVolumeBroadcastsTest {
    @Test
    fun theAlarmStreamConcernsTheAlarm() {
        assertThat(AlarmVolumeBroadcasts.concernsAlarm(AudioManager.STREAM_ALARM)).isTrue()
    }

    @Test
    fun aMissingStreamIsReadAgainOutOfCaution() {
        assertThat(AlarmVolumeBroadcasts.concernsAlarm(-1)).isTrue()
    }

    @Test
    fun otherStreamsDoNotConcernTheAlarm() {
        assertThat(AlarmVolumeBroadcasts.concernsAlarm(AudioManager.STREAM_MUSIC)).isFalse()
        assertThat(AlarmVolumeBroadcasts.concernsAlarm(AudioManager.STREAM_RING)).isFalse()
        assertThat(AlarmVolumeBroadcasts.concernsAlarm(AudioManager.STREAM_NOTIFICATION)).isFalse()
    }
}
