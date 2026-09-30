package com.niumi.system.readiness

import com.google.common.truth.Truth.assertThat
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.session.fakes.FakeUptimeClock
import org.junit.Test

/** Étape 25 : la fenêtre où Android relie encore le service d'accessibilité après déverrouillage. */
class UnlockSettlingTest {
    private val unlockState = FakeUnlockState(isUserUnlocked = false)
    private val uptimeClock = FakeUptimeClock(elapsedMillis = 40_000L)
    private val settling = UnlockSettling(unlockState, uptimeClock)

    @Test
    fun noWindowWhileTheDeviceIsLocked() {
        assertThat(settling.isSettling()).isFalse()
        assertThat(settling.remainingMillis()).isEqualTo(0)
    }

    /** Le début est la première lecture déverrouillée, pas le démarrage : un verrouillage long n'use pas la fenêtre. */
    @Test
    fun theWindowOpensAtTheFirstUnlockedReading() {
        uptimeClock.elapsedMillis = 600_000L
        unlockState.isUserUnlocked = true

        assertThat(settling.remainingMillis()).isEqualTo(UnlockSettling.GRACE_MILLIS)

        uptimeClock.elapsedMillis += 10_000L
        assertThat(settling.remainingMillis()).isEqualTo(UnlockSettling.GRACE_MILLIS - 10_000L)
        assertThat(settling.isSettling()).isTrue()
    }

    @Test
    fun theWindowClosesAfterTheGracePeriodAndStaysClosed() {
        unlockState.isUserUnlocked = true
        settling.isSettling()

        uptimeClock.elapsedMillis += UnlockSettling.GRACE_MILLIS
        assertThat(settling.isSettling()).isFalse()

        uptimeClock.elapsedMillis += 3_600_000L
        assertThat(settling.isSettling()).isFalse()
    }
}
