package com.niumi.system.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Surveillance de la notification de sonnerie (SPEC_ANDROID §10.2, §10.4). Les deux exigences ne
 * se contredisent que si l'on ignore **qui** a fait disparaître la notification : le critère est
 * donc l'état de l'appareil, et non le rang de la disparition.
 */
class RingingNotificationWatchTest {
    @Test
    fun aPostedNotificationNeedsNothing() {
        assertThat(RingingNotificationWatch.decide(isPosted = true, deviceInUse = true))
            .isEqualTo(RingingNotificationAction.NONE)
        assertThat(RingingNotificationWatch.decide(isPosted = true, deviceInUse = false))
            .isEqualTo(RingingNotificationAction.NONE)
    }

    /**
     * Le scénario même pour lequel §10.2 existe : l'utilisateur dort, la notification a disparu,
     * et le plein écran est le seul mécanisme qu'Android autorise pour rouvrir l'écran de réveil
     * depuis l'arrière-plan. Sans lui, plus aucun accès au scan (§11.2).
     */
    @Test
    fun aSleepingDeviceGetsTheScreenBack() {
        val action = RingingNotificationWatch.decide(isPosted = false, deviceInUse = false)

        assertThat(action).isEqualTo(RingingNotificationAction.REPUBLISH_WITH_FULL_SCREEN)
    }

    /**
     * §10.4 : « une activité impossible à quitter serait hostile ». Un utilisateur réveillé qui
     * écarte la notification ne doit pas voir l'écran lui revenir dessus — mais la notification,
     * elle, revient, avec son `contentIntent` : l'accès au scan n'est jamais perdu.
     */
    @Test
    fun aDeviceInUseKeepsTheNotificationWithoutSeizingTheScreen() {
        val action = RingingNotificationWatch.decide(isPosted = false, deviceInUse = true)

        assertThat(action).isEqualTo(RingingNotificationAction.REPUBLISH_SILENTLY)
    }

    /**
     * La règle n'a aucun état à épuiser : la garde « plein écran une seule fois » qu'elle remplace
     * pouvait être consommée par une absence transitoire de `getActiveNotifications()`, si bien
     * que la disparition réellement subie pendant le sommeil n'obtenait plus que la republication
     * silencieuse (mesuré sur appareil à l'étape 17).
     */
    @Test
    fun theSameDeviceStateAlwaysYieldsTheSameDecision() {
        repeat(3) {
            assertThat(RingingNotificationWatch.decide(isPosted = false, deviceInUse = false))
                .isEqualTo(RingingNotificationAction.REPUBLISH_WITH_FULL_SCREEN)
        }
        repeat(3) {
            assertThat(RingingNotificationWatch.decide(isPosted = false, deviceInUse = true))
                .isEqualTo(RingingNotificationAction.REPUBLISH_SILENTLY)
        }
    }
}
