package com.niumi.system.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Surveillance de la notification de sonnerie (SPEC_ANDROID §10.2). Depuis le Lot 9, elle est le
 * premier chemin vers le scan : absente, elle revient, avec son seul `contentIntent`, quel que
 * soit l'état de l'appareil — aucun écran n'est imposé (§10.4).
 */
class RingingNotificationWatchTest {
    @Test
    fun aPostedNotificationIsLeftAlone() {
        assertThat(RingingNotificationWatch.decide(isPosted = true))
            .isEqualTo(RingingNotificationAction.NONE)
    }

    /**
     * Balayée par l'utilisateur, téléphone verrouillé ou non : elle revient. La décision ne prend
     * plus l'état de l'appareil en entrée (Lot 9) — la garde « plein écran une seule fois » de
     * l'étape 17 avait été consommée par une absence transitoire de `getActiveNotifications()`.
     */
    @Test
    fun aMissingNotificationIsRepublished() {
        assertThat(RingingNotificationWatch.decide(isPosted = false))
            .isEqualTo(RingingNotificationAction.REPUBLISH)
    }

    @Test
    fun theWatchOnlyKnowsTwoActions() {
        assertThat(RingingNotificationAction.entries)
            .containsExactly(RingingNotificationAction.NONE, RingingNotificationAction.REPUBLISH)
    }
}
