package com.niumi.system.readiness

import com.google.common.truth.Truth.assertThat
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.session.fakes.FakeUptimeClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Étape 25 : le verdict suspendu pendant la fenêtre de liaison est rejoué à sa fin, une seule fois. */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineReadinessRecheckTest {
    private val unlockState = FakeUnlockState(isUserUnlocked = true)
    private val uptimeClock = FakeUptimeClock()
    private val settling = UnlockSettling(unlockState, uptimeClock)
    private var evaluations = 0
    private val trigger = ForegroundReadinessTrigger { evaluations++ }

    @Test
    fun theRecheckRunsOnceAtTheEndOfTheWindow() =
        runTest {
            val recheck = CoroutineReadinessRecheck(settling, { trigger }, StandardTestDispatcher(testScheduler))

            recheck.recheckAfterSettling()
            recheck.recheckAfterSettling()
            advanceTimeBy(UnlockSettling.GRACE_MILLIS - 1)
            runCurrent()
            assertThat(evaluations).isEqualTo(0)

            advanceTimeBy(1)
            runCurrent()
            assertThat(evaluations).isEqualTo(1)
        }

    @Test
    fun noRecheckOutsideTheWindow() =
        runTest {
            val recheck = CoroutineReadinessRecheck(settling, { trigger }, StandardTestDispatcher(testScheduler))
            settling.isSettling()
            uptimeClock.elapsedMillis += UnlockSettling.GRACE_MILLIS

            recheck.recheckAfterSettling()
            advanceTimeBy(UnlockSettling.GRACE_MILLIS * 2)
            runCurrent()

            assertThat(evaluations).isEqualTo(0)
        }
}
