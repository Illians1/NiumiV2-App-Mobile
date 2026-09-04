package com.niumi.system.notification

import android.app.Notification
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §10.3 — textes exacts, `CATEGORY_ALARM`, `ongoing`, full-screen intent, et
 * surtout **aucune action** : garde-fou du principe « aucune action d'arrêt dans le parcours
 * de sonnerie » (SPEC_ANDROID §3, §10.2, §10.4), vérifié automatiquement à chaque modification.
 */
class RingingNotificationSpecsTest {
    @Test
    fun ringingSpecMatchesExactTextsAndCategory() {
        val spec = RingingNotificationSpecs.ringing()

        assertThat(spec.channelId).isEqualTo(NiumiNotificationChannels.alarmRinging.id)
        assertThat(spec.category).isEqualTo(Notification.CATEGORY_ALARM)
        assertThat(spec.title).isEqualTo("Alarme Niumi en cours")
        assertThat(spec.text).isEqualTo("Scanne ton boîtier pour terminer la session.")
        assertThat(spec.ongoing).isTrue()
        assertThat(spec.hasFullScreenIntent).isTrue()
    }

    @Test
    fun ringingSpecHasNoAction() {
        val spec = RingingNotificationSpecs.ringing()

        assertThat(spec.actions).isEmpty()
    }
}
