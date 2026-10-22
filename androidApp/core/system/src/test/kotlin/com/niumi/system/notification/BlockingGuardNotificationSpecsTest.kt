package com.niumi.system.notification

import android.app.Notification
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §12.5 : textes exacts de la garde avant l'alarme, notification d'état silencieuse et
 * permanente, **aucune action** — rien n'arrête une session hors du scan.
 */
class BlockingGuardNotificationSpecsTest {
    @Test
    fun beforeBedtimeItAnnouncesTheWakeAndTheBlockingTimes() {
        val spec = BlockingGuardNotificationSpecs.beforeBlocking("07:00", "22:30")

        assertThat(spec.title).isEqualTo("Réveil programmé à 07:00")
        assertThat(spec.text).isEqualTo("Tes applications choisies seront bloquées à partir de 22:30.")
    }

    @Test
    fun withoutABlockingTimeItSaysAtBedtime() {
        assertThat(BlockingGuardNotificationSpecs.beforeBlocking("07:00", null).text)
            .isEqualTo("Tes applications choisies seront bloquées au coucher.")
    }

    /** Décision du 2026-10-22 : la notification parle du réveil, et le blocage dure jusqu'au scan. */
    @Test
    fun duringTheBlockingTheTitleCarriesTheWakeTime() {
        val spec = BlockingGuardNotificationSpecs.blocking("07:00")

        assertThat(spec.title).isEqualTo("Réveil prévu à 07:00")
        assertThat(spec.text).isEqualTo("Tes applications choisies restent bloquées jusqu'au scan de ton Niumi Point.")
    }

    /** Service relancé par le système avant d'avoir relu la session : pas d'heure inventée. */
    @Test
    fun withoutAWakeTimeTheTitlesSayOnlyThePhase() {
        assertThat(BlockingGuardNotificationSpecs.beforeBlocking(null, null).title).isEqualTo("Réveil programmé")
        assertThat(BlockingGuardNotificationSpecs.blocking(null).title).isEqualTo("Réveil prévu")
    }

    /** Relance sans commande : rien qui serait faux avant comme pendant le blocage. */
    @Test
    fun anUnknownPhaseSaysOnlyWhatIsTrueInBoth() {
        val spec = BlockingGuardNotificationSpecs.unknownPhase()

        assertThat(spec.title).isEqualTo("Réveil programmé")
        assertThat(spec.text).isEqualTo("Ta session Niumi est en cours.")
    }

    @Test
    fun allArePermanentSilentStatusNotificationsWithoutAction() {
        listOf(
            BlockingGuardNotificationSpecs.beforeBlocking("07:00", "22:30"),
            BlockingGuardNotificationSpecs.blocking("07:00"),
            BlockingGuardNotificationSpecs.unknownPhase(),
        ).forEach { spec ->
            assertThat(spec.channelId).isEqualTo(NiumiNotificationChannels.blockingGuard.id)
            assertThat(spec.category).isEqualTo(Notification.CATEGORY_STATUS)
            assertThat(spec.ongoing).isTrue()
            assertThat(spec.visibilityPublic).isTrue()
            assertThat(spec.alertOnlyOnce).isTrue()
            assertThat(spec.actions).isEmpty()
        }
    }
}
