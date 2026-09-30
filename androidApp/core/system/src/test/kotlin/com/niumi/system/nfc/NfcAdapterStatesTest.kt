package com.niumi.system.nfc

import android.nfc.NfcAdapter
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Écart 10 : le diagnostic n'est rejoué que sur un état stable du NFC. Rejoué sur une
 * transition, il lirait encore l'ancien état — le défaut mesuré le 2026-09-29.
 */
class NfcAdapterStatesTest {
    @Test
    fun onAndOffAreSettled() {
        assertThat(NfcAdapterStates.isSettled(NfcAdapter.STATE_ON)).isTrue()
        assertThat(NfcAdapterStates.isSettled(NfcAdapter.STATE_OFF)).isTrue()
    }

    @Test
    fun transitionsAreNotSettled() {
        assertThat(NfcAdapterStates.isSettled(NfcAdapter.STATE_TURNING_ON)).isFalse()
        assertThat(NfcAdapterStates.isSettled(NfcAdapter.STATE_TURNING_OFF)).isFalse()
    }

    @Test
    fun aMissingStateIsNotSettled() {
        assertThat(NfcAdapterStates.isSettled(-1)).isFalse()
    }
}
