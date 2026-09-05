package com.niumi.feature.ringing.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanOutcome
import org.junit.Test

/**
 * SPEC_ANDROID §10.4 : textes exacts par état, repris mot pour mot. L'état NFC réel (issu du
 * moteur commun) n'existe pas encore (étape 4/17) ; [AlarmRingingPhase] anticipe les trois
 * phases atteignables sur Android pour que l'écran affiche déjà le bon texte quand elles
 * seront alimentées.
 */
class AlarmScreenStateTest {
    @Test
    fun ringingPhaseShowsTheStopInstructionText() {
        val state = AlarmScreenState.from(phase = AlarmRingingPhase.RINGING, deviceLocked = false)

        assertThat(state.instructionText).isEqualTo("Scanne ton boîtier Niumi pour arrêter l'alarme.")
    }

    @Test
    fun triggeredAwaitingNfcPhaseShowsThePassedTimeText() {
        val state =
            AlarmScreenState.from(phase = AlarmRingingPhase.TRIGGERED_AWAITING_NFC, deviceLocked = false)

        assertThat(state.instructionText)
            .isEqualTo(
                "L'heure de ton réveil est passée. Scanne ton boîtier Niumi pour débloquer tes applications.",
            )
    }

    @Test
    fun awaitingNfcPhaseShowsTheSoundStoppedText() {
        val state = AlarmScreenState.from(phase = AlarmRingingPhase.AWAITING_NFC, deviceLocked = false)

        assertThat(state.instructionText)
            .isEqualTo(
                "Le son est arrêté, mais tes applications restent bloquées. " +
                    "Scanne ton boîtier Niumi pour terminer la session.",
            )
    }

    @Test
    fun deviceLockedOverridesTheInstructionTextRegardlessOfPhase() {
        val state = AlarmScreenState.from(phase = AlarmRingingPhase.RINGING, deviceLocked = true)

        assertThat(state.instructionText).isEqualTo("Déverrouille ton téléphone, puis approche-le du boîtier.")
    }

    // SPEC_ANDROID §11.2, §4.4 : ordre de priorité rang 1 à 6, du plus au moins bloquant.

    @Test
    fun absentNfcTakesPriorityOverEverythingElse() {
        val state =
            AlarmScreenState.from(
                phase = AlarmRingingPhase.RINGING,
                deviceLocked = true,
                nfcAvailability = NfcAvailability.ABSENT,
                lastScanOutcome = ScanOutcome.Unreadable,
            )

        assertThat(state.instructionText).isEqualTo("Cet appareil ne prend pas en charge le NFC.")
        assertThat(state.showsNfcSettingsShortcut).isFalse()
    }

    @Test
    fun disabledNfcTakesPriorityOverLockedDeviceAndShowsSettingsShortcut() {
        val state =
            AlarmScreenState.from(
                phase = AlarmRingingPhase.RINGING,
                deviceLocked = true,
                nfcAvailability = NfcAvailability.DISABLED,
            )

        assertThat(state.instructionText)
            .isEqualTo("Le NFC est désactivé. Active-le pour scanner ton boîtier.")
        assertThat(state.showsNfcSettingsShortcut).isTrue()
    }

    @Test
    fun lockedDeviceTakesPriorityOverScanOutcome() {
        val state =
            AlarmScreenState.from(
                phase = AlarmRingingPhase.RINGING,
                deviceLocked = true,
                nfcAvailability = NfcAvailability.ENABLED,
                lastScanOutcome = ScanOutcome.Unreadable,
            )

        assertThat(state.instructionText).isEqualTo("Déverrouille ton téléphone, puis approche-le du boîtier.")
    }

    @Test
    fun unreadableScanOutcomeShowsTheExactSpecText() {
        val state =
            AlarmScreenState.from(
                phase = AlarmRingingPhase.RINGING,
                deviceLocked = false,
                lastScanOutcome = ScanOutcome.Unreadable,
            )

        assertThat(state.instructionText).isEqualTo("Boîtier non reconnu. Réessaie.")
    }

    @Test
    fun unknownBoxScanOutcomeTakesPriorityOverPhaseText() {
        val state =
            AlarmScreenState.from(
                phase = AlarmRingingPhase.RINGING,
                deviceLocked = false,
                lastScanOutcome = ScanOutcome.UnknownBox,
            )

        assertThat(state.instructionText).isEqualTo("Ce boîtier n'est pas celui de ta session.")
    }
}
