package com.niumi.system.notification

import android.app.Notification
import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §10.3 (`niumi_alarm_ringing`) et §10.5 (`niumi_session_awaiting_scan`), les deux
 * canaux créés dès l'étape 3 même si le second n'est utilisé qu'à l'étape 17.
 */
class NiumiNotificationChannelsTest {
    @Test
    fun ringingChannelIsHighImportanceWithoutSoundAndPublicVisibility() {
        val spec = NiumiNotificationChannels.alarmRinging

        assertThat(spec.id).isEqualTo("niumi_alarm_ringing")
        assertThat(spec.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(spec.hasSound).isFalse()
        assertThat(spec.visibilityPublic).isTrue()
    }

    @Test
    fun awaitingScanChannelHasNoSoundAndNoVibration() {
        val spec = NiumiNotificationChannels.sessionAwaitingScan

        assertThat(spec.id).isEqualTo("niumi_session_awaiting_scan")
        assertThat(spec.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(spec.hasSound).isFalse()
        assertThat(spec.hasVibration).isFalse()
        assertThat(spec.visibilityPublic).isTrue()
    }

    /** §12.5 : permanente pendant le blocage, elle ne doit jamais sonner ni s'afficher en bannière. */
    @Test
    fun blockingGuardChannelIsLowImportanceAndSilent() {
        val spec = NiumiNotificationChannels.blockingGuard

        assertThat(spec.id).isEqualTo("niumi_blocking_guard")
        assertThat(spec.importance).isEqualTo(NotificationManager.IMPORTANCE_LOW)
        assertThat(spec.hasSound).isFalse()
        assertThat(spec.hasVibration).isFalse()
        assertThat(spec.visibilityPublic).isTrue()
    }

    @Test
    fun everyChannelIsRegisteredWithADistinctId() {
        val ids = NiumiNotificationChannels.all.map { it.id }

        assertThat(ids).containsNoDuplicates()
        assertThat(ids).contains(NiumiNotificationChannels.blockingGuard.id)
    }

    @Test
    fun theTwoChannelsHaveDistinctIds() {
        assertThat(NiumiNotificationChannels.alarmRinging.id)
            .isNotEqualTo(NiumiNotificationChannels.sessionAwaitingScan.id)
    }

    @Test
    fun categoryConstantIsAlarm() {
        // Contrôle de non-régression : CATEGORY_ALARM est bien la constante utilisée par les
        // specs de notification (RingingNotificationSpecsTest la vérifie sur le spec réel).
        assertThat(Notification.CATEGORY_ALARM).isEqualTo("alarm")
    }
}
