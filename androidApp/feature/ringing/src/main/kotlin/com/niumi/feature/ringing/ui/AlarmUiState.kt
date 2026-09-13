package com.niumi.feature.ringing.ui

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanOutcome

/** Où l'écran de réveil renvoie l'utilisateur quand la session atteint un état final. */
enum class AlarmExitDestination {
    /** Écran 10, après `COMPLETED` (SPEC_ANDROID §15). */
    COMPLETED,

    /** Écran 11, après `CANCELLED`. */
    CANCELLED,

    /**
     * `FAILED` n'a pas d'écran dans §15 : il est réservé à une activation qui n'a jamais abouti
     * (§18), donc inatteignable depuis l'écran de réveil. Classé explicitement plutôt que laissé
     * à un `else`, l'exhaustivité du `when` étant la propriété recherchée.
     */
    HOME,
}

/**
 * Ce que l'écran de réveil affiche, pour un état de session donné (SPEC_ANDROID §10.4).
 *
 * [Loading] existe pour une raison précise : `SessionSnapshotPublisher` vit en mémoire et vaut
 * `null` dans tout processus neuf — et le processus est **toujours** neuf quand le plein écran
 * ouvre l'activité après un réveil. Traiter ce `null` comme « pas de session » fermerait l'écran
 * instantanément, privant l'utilisateur du seul accès au scan. Seule une absence **confirmée par
 * la persistance** ferme l'écran ; un snapshot illisible ne la confirme pas (SPEC_CORE_KMP §13).
 */
sealed interface AlarmUiState {
    data object Loading : AlarmUiState

    data class Visible(
        val screen: AlarmScreenState,
    ) : AlarmUiState

    data class Exit(
        val destination: AlarmExitDestination,
    ) : AlarmUiState

    data object Close : AlarmUiState

    companion object {
        fun forSession(
            sessionState: SessionStateDto,
            deviceLocked: Boolean,
            nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
            lastScanOutcome: ScanOutcome? = null,
            releaseSteps: List<ReleaseStep> = emptyList(),
        ): AlarmUiState =
            when (sessionState) {
                SessionStateDto.RINGING,
                SessionStateDto.AWAITING_NFC,
                SessionStateDto.TRIGGERED_AWAITING_NFC,
                SessionStateDto.RELEASING,
                -> {
                    Visible(
                        AlarmScreenState.from(
                            sessionState = sessionState,
                            deviceLocked = deviceLocked,
                            nfcAvailability = nfcAvailability,
                            lastScanOutcome = lastScanOutcome,
                            releaseSteps = releaseSteps,
                        ),
                    )
                }

                SessionStateDto.COMPLETED -> {
                    Exit(AlarmExitDestination.COMPLETED)
                }

                SessionStateDto.CANCELLED -> {
                    Exit(AlarmExitDestination.CANCELLED)
                }

                SessionStateDto.FAILED -> {
                    Exit(AlarmExitDestination.HOME)
                }

                // Avant la sonnerie, l'écran de réveil n'a rien à montrer : l'écran 7 est le seul
                // écran atteignable pendant une session armée (§10.4).
                SessionStateDto.PREPARING, SessionStateDto.ARMED -> {
                    Close
                }
            }
    }
}
