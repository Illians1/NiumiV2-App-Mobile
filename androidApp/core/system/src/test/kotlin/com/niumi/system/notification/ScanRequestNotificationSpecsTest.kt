package com.niumi.system.notification

import android.app.Notification
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §10.5 — textes exacts, `CATEGORY_ALARM`, `ongoing`, et surtout **aucun full-screen
 * intent et aucune action** : cette notification ne doit jamais rallumer l'écran ni simuler une
 * alarme active (fenêtre de grâce de 15 minutes qui empêche justement une sonnerie tardive).
 */
class ScanRequestNotificationSpecsTest {
    @Test
    fun awaitingScanSpecMatchesExactTextsAndCategory() {
        val spec = ScanRequestNotificationSpecs.awaitingScan()

        assertThat(spec.channelId).isEqualTo(NiumiNotificationChannels.sessionAwaitingScan.id)
        assertThat(spec.category).isEqualTo(Notification.CATEGORY_ALARM)
        assertThat(spec.title).isEqualTo("Ton réveil Niumi est passé")
        assertThat(spec.text).isEqualTo("Scanne ton boîtier pour débloquer tes applications.")
        assertThat(spec.ongoing).isTrue()
    }

    @Test
    fun awaitingScanSpecHasNoFullScreenIntentAndNoAction() {
        val spec = ScanRequestNotificationSpecs.awaitingScan()

        assertThat(spec.hasFullScreenIntent).isFalse()
        assertThat(spec.actions).isEmpty()
    }
}
