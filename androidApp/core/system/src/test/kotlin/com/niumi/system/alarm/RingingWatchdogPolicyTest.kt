package com.niumi.system.alarm

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * SPEC_ANDROID §10.2 (étape 20) : `RINGING` est le seul état qui arme l'alarme de secours, tous
 * les autres la désarment. Preuve d'exhaustivité sur l'énumération entière, même patron que
 * `SessionScanStatesTest` : un état ajouté sans être classé fait échouer la compilation du `when`
 * de [RingingWatchdogPolicy.decide], jamais ce test.
 */
class RingingWatchdogPolicyTest {
    @Test
    fun onlyRingingArmsTheWatchdog() {
        assertThat(RingingWatchdogPolicy.decide(SessionStateDto.RINGING)).isEqualTo(WatchdogAction.Arm)
    }

    @Test
    fun everyOtherStateDisarmsTheWatchdog() {
        val others = SessionStateDto.entries.filterNot { it == SessionStateDto.RINGING }

        assertThat(others).isNotEmpty()
        for (state in others) {
            assertThat(RingingWatchdogPolicy.decide(state)).isEqualTo(WatchdogAction.Disarm)
        }
    }
}
