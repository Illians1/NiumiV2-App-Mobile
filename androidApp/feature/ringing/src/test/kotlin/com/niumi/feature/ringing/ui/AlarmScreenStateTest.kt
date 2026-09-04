package com.niumi.feature.ringing.ui

import com.google.common.truth.Truth.assertThat
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
}
