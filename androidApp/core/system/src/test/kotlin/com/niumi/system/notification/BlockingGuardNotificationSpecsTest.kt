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
        val spec =
            BlockingGuardNotificationSpecs.beforeBlocking(
                GuardMoment("7:00", day = null),
                GuardMoment("22:30", day = null),
            )

        assertThat(spec.title).isEqualTo("Réveil à 7:00")
        assertThat(spec.text).isEqualTo("Tes applications choisies seront bloquées à partir de 22:30.")
    }

    /** Décision du 2026-10-25 : un réveil armé des jours à l'avance dit son jour. */
    @Test
    fun aWakeDaysAheadNamesTheDays() {
        val spec =
            BlockingGuardNotificationSpecs.beforeBlocking(
                GuardMoment("0:30", day = "dimanche"),
                GuardMoment("11:45", day = "samedi"),
            )

        assertThat(spec.title).isEqualTo("Réveil dimanche à 0:30")
        assertThat(spec.text).isEqualTo("Tes applications choisies seront bloquées samedi à partir de 11:45.")
    }

    @Test
    fun aWakeTheNextDayNamesItsDay() {
        val spec =
            BlockingGuardNotificationSpecs.beforeBlocking(
                GuardMoment("7:00", day = "lundi"),
                GuardMoment("22:30", day = null),
            )

        assertThat(spec.title).isEqualTo("Réveil lundi à 7:00")
        assertThat(spec.text).isEqualTo("Tes applications choisies seront bloquées à partir de 22:30.")
    }

    @Test
    fun withoutABlockingTimeItSaysAtBedtime() {
        assertThat(BlockingGuardNotificationSpecs.beforeBlocking(GuardMoment("7:00", null), null).text)
            .isEqualTo("Tes applications choisies seront bloquées au coucher.")
    }

    /** Décision du 2026-10-22 : la notification parle du réveil, et le blocage dure jusqu'au scan. */
    @Test
    fun duringTheBlockingTheTitleCarriesTheWakeTime() {
        val spec = BlockingGuardNotificationSpecs.blocking("7:00")

        assertThat(spec.title).isEqualTo("Réveil prévu à 7:00")
        assertThat(spec.text).isEqualTo("Tes applications choisies restent bloquées jusqu'au scan de ton Niumi Spot.")
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
        assertThat(spec.text).isEqualTo("Tes applications choisies seront bloquées jusqu'au scan de ton Niumi Spot.")
    }

    /** `SPECIFICATION.md` §14 (décision du 2026-10-23) : les notifications ne disent jamais « session ». */
    @Test
    fun noTextSpeaksOfASession() {
        val specs =
            listOf(
                BlockingGuardNotificationSpecs.beforeBlocking(
                    GuardMoment("7:00", "lundi"),
                    GuardMoment("22:30", null),
                ),
                BlockingGuardNotificationSpecs.beforeBlocking(null, null),
                BlockingGuardNotificationSpecs.blocking("7:00"),
                BlockingGuardNotificationSpecs.blocking(null),
                BlockingGuardNotificationSpecs.unknownPhase(),
            )

        assertThat(specs.flatMap { listOf(it.title, it.text) }.filter { "session" in it.lowercase() }).isEmpty()
    }

    @Test
    fun allArePermanentSilentStatusNotificationsWithoutAction() {
        listOf(
            BlockingGuardNotificationSpecs.beforeBlocking(GuardMoment("7:00", null), GuardMoment("22:30", null)),
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
