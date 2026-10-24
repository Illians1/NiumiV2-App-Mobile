package com.niumi.system.notification

import android.app.Notification
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §10.5 — textes exacts, `CATEGORY_ALARM`, `ongoing`, et surtout **aucune action** :
 * cette notification ne doit jamais simuler une alarme active (fenêtre de grâce de 15 minutes qui
 * empêche justement une sonnerie tardive). Aucune notification de Niumi ne porte de plein écran
 * depuis le Lot 9 : le test instrumenté le vérifie sur la vraie `Notification`.
 */
class ScanRequestNotificationSpecsTest {
    @Test
    fun awaitingScanSpecMatchesExactTextsAndCategory() {
        val spec = ScanRequestNotificationSpecs.awaitingScan()

        assertThat(spec.channelId).isEqualTo(NiumiNotificationChannels.sessionAwaitingScan.id)
        assertThat(spec.category).isEqualTo(Notification.CATEGORY_ALARM)
        assertThat(spec.title).isEqualTo("Ton réveil Niumi est passé")
        assertThat(spec.text).isEqualTo("Scanne ton Niumi Spot pour retrouver tes applis.")
        assertThat(spec.ongoing).isTrue()
    }

    @Test
    fun awaitingScanSpecHasNoAction() {
        val spec = ScanRequestNotificationSpecs.awaitingScan()

        assertThat(spec.actions).isEmpty()
        // Publiée une fois par PRESENT_SCAN_REQUEST, jamais rejouée chaque minute : rien à taire.
        assertThat(spec.alertOnlyOnce).isFalse()
    }
}
