package com.niumi.system.blocking.guard

import com.google.common.truth.Truth.assertWithMessage
import com.niumi.system.notification.RingingNotificationAction
import org.junit.Test

/**
 * SPEC_ANDROID §12.5 (décision de Mehdi du 2026-10-22) : une notification écartée n'est ramenée
 * par la garde qu'en attente de scan. Avant l'alarme, elle ne fait qu'informer ; pendant la
 * sonnerie, c'est `AlarmRingingService` qui ramène la notification partagée (§10.2).
 */
class BlockingGuardNotificationWatchTest {
    @Test
    fun onlyAMissingScanRequestIsBroughtBack() {
        BlockingGuardPhase.entries.forEach { phase ->
            val expected =
                if (phase == BlockingGuardPhase.AWAITING_SCAN) {
                    RingingNotificationAction.REPUBLISH
                } else {
                    RingingNotificationAction.NONE
                }
            assertWithMessage(phase.name)
                .that(BlockingGuardNotificationWatch.decide(phase, isPosted = false))
                .isEqualTo(expected)
        }
    }

    @Test
    fun aPostedNotificationIsLeftAlone() {
        BlockingGuardPhase.entries.forEach { phase ->
            assertWithMessage(phase.name)
                .that(BlockingGuardNotificationWatch.decide(phase, isPosted = true))
                .isEqualTo(RingingNotificationAction.NONE)
        }
    }

    @Test
    fun onlyTheScanRequestPhaseIsWatched() {
        BlockingGuardPhase.entries.forEach { phase ->
            assertWithMessage(phase.name)
                .that(BlockingGuardNotificationWatch.isWatched(phase))
                .isEqualTo(phase == BlockingGuardPhase.AWAITING_SCAN)
        }
    }
}
