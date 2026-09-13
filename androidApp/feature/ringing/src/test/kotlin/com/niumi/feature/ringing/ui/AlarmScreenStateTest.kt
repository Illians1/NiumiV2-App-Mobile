package com.niumi.feature.ringing.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanOutcome
import org.junit.Test

/**
 * Textes de l'écran de réveil (SPEC_ANDROID §10.4, §11.2) et ordre de priorité entre eux. Depuis
 * l'étape 17, l'état vient du moteur (`SessionStateDto`) et non plus d'un enum local.
 */
class AlarmScreenStateTest {
    private fun stateFor(
        sessionState: SessionStateDto = SessionStateDto.RINGING,
        deviceLocked: Boolean = false,
        nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
        lastScanOutcome: ScanOutcome? = null,
        releaseSteps: List<ReleaseStep> = emptyList(),
    ) = AlarmScreenState.from(sessionState, deviceLocked, nfcAvailability, lastScanOutcome, releaseSteps)

    @Test
    fun ringingAsksForTheScanToStopTheAlarm() {
        assertThat(stateFor(SessionStateDto.RINGING).instructionText)
            .isEqualTo("Scanne ton boîtier Niumi pour arrêter l'alarme.")
    }

    /** §10.4 : le texte ne doit pas affirmer que le son n'a jamais sonné. */
    @Test
    fun aMissedTriggerAsksForTheScanToUnblockApplications() {
        assertThat(stateFor(SessionStateDto.TRIGGERED_AWAITING_NFC).instructionText)
            .isEqualTo(
                "L'heure de ton réveil est passée. " +
                    "Scanne ton boîtier Niumi pour débloquer tes applications.",
            )
    }

    @Test
    fun aStoppedSoundStillAsksForTheScanToEndTheSession() {
        assertThat(stateFor(SessionStateDto.AWAITING_NFC).instructionText)
            .isEqualTo(
                "Le son est arrêté, mais tes applications restent bloquées. " +
                    "Scanne ton boîtier Niumi pour terminer la session.",
            )
    }

    @Test
    fun releasingAnnouncesTheCleanupInsteadOfAScan() {
        assertThat(stateFor(SessionStateDto.RELEASING).instructionText)
            .isEqualTo("Ton scan est validé. Niumi termine la session.")
    }

    /** Le scan a déjà eu lieu : ni le verrouillage ni l'état du NFC ne décrivent plus rien. */
    @Test
    fun releasingOutranksTheLockedAndNfcMessages() {
        val state =
            stateFor(
                sessionState = SessionStateDto.RELEASING,
                deviceLocked = true,
                nfcAvailability = NfcAvailability.DISABLED,
                lastScanOutcome = ScanOutcome.UnknownBox,
            )

        assertThat(state.instructionText).isEqualTo("Ton scan est validé. Niumi termine la session.")
        assertThat(state.showsNfcSettingsShortcut).isFalse()
    }

    @Test
    fun releasingCarriesItsCleanupSteps() {
        val steps = ReleaseProgress.from(replayableEffects = emptyList())

        val state = stateFor(SessionStateDto.RELEASING, releaseSteps = steps)

        assertThat(state.releaseSteps).isEqualTo(steps)
    }

    @Test
    fun aLockedDeviceAsksToUnlockFirst() {
        assertThat(stateFor(deviceLocked = true).instructionText)
            .isEqualTo("Déverrouille ton téléphone, puis approche-le du boîtier.")
    }

    @Test
    fun missingNfcHardwareOutranksEverythingAndOffersNoShortcut() {
        val state =
            stateFor(
                deviceLocked = true,
                nfcAvailability = NfcAvailability.ABSENT,
                lastScanOutcome = ScanOutcome.UnknownBox,
            )

        assertThat(state.instructionText).isEqualTo("Cet appareil ne prend pas en charge le NFC.")
        assertThat(state.showsNfcSettingsShortcut).isFalse()
    }

    @Test
    fun disabledNfcOutranksTheLockedMessageAndOffersTheShortcut() {
        val state = stateFor(deviceLocked = true, nfcAvailability = NfcAvailability.DISABLED)

        assertThat(state.instructionText)
            .isEqualTo("Le NFC est désactivé. Active-le pour scanner ton boîtier.")
        assertThat(state.showsNfcSettingsShortcut).isTrue()
    }

    @Test
    fun aLockedDeviceOutranksAScanOutcome() {
        val state = stateFor(deviceLocked = true, lastScanOutcome = ScanOutcome.Unreadable)

        assertThat(state.instructionText)
            .isEqualTo("Déverrouille ton téléphone, puis approche-le du boîtier.")
    }

    @Test
    fun anUnreadableTagAsksToRetry() {
        assertThat(stateFor(lastScanOutcome = ScanOutcome.Unreadable).instructionText)
            .isEqualTo("Boîtier non reconnu. Réessaie.")
    }

    @Test
    fun anUnknownBoxSaysItIsNotTheSessionBox() {
        assertThat(stateFor(lastScanOutcome = ScanOutcome.UnknownBox).instructionText)
            .isEqualTo("Ce boîtier n'est pas celui de ta session.")
    }

    /** Les quatre états affichables portent tous une instruction : aucun écran muet. */
    @Test
    fun everyDisplayableStateHasAnInstruction() {
        listOf(
            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            SessionStateDto.RELEASING,
        ).forEach { state ->
            assertThat(stateFor(state).instructionText).isNotEmpty()
        }
    }
}
