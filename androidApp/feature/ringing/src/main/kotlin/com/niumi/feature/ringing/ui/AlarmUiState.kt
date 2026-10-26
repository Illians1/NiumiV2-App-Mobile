package com.niumi.feature.ringing.ui

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanSheetState

/** Où l'écran de réveil renvoie l'utilisateur quand la session suivie atteint un état final. */
sealed interface AlarmExitDestination {
    /** Écran 11 · Réussite, qui relit cette session par son identifiant (étape 46). */
    data class Success(
        val sessionId: String,
    ) : AlarmExitDestination

    /**
     * L'accueil : après `CANCELLED` (l'écran « Session annulée » a disparu à l'étape 46), un
     * désarmement ou `FAILED` — réservé à une activation qui n'a jamais abouti (§18), donc
     * inatteignable depuis l'écran de réveil.
     */
    data object Home : AlarmExitDestination
}

/**
 * Ce que l'écran de réveil affiche, pour un état de session donné (SPEC_ANDROID §10.4).
 *
 * [Loading] existe pour une raison précise : `SessionSnapshotPublisher` vit en mémoire et vaut
 * `null` dans tout processus neuf — et le processus peut être neuf quand la notification de
 * sonnerie ouvre l'activité après un réveil (mort du processus, reconstruction du service).
 * Traiter ce `null` comme « pas de session » fermerait l'écran instantanément, privant
 * l'utilisateur du seul accès au scan. Seule une absence **confirmée par
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
            sessionId: String,
            nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
            sheet: ScanSheetState = ScanSheetState.Hidden,
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
                            nfcAvailability = nfcAvailability,
                            sheet = sheet,
                        ),
                    )
                }

                SessionStateDto.COMPLETED -> {
                    Exit(AlarmExitDestination.Success(sessionId))
                }

                // Un désarmement (contrat 1.5) précède le coucher : rien à montrer après, comme
                // une activation échouée ou une annulation.
                SessionStateDto.CANCELLED, SessionStateDto.DISARMED, SessionStateDto.FAILED -> {
                    Exit(AlarmExitDestination.Home)
                }

                // Avant la sonnerie, l'écran de réveil n'a rien à montrer : l'écran 7 est le seul
                // écran atteignable pendant une session armée (§10.4).
                SessionStateDto.PREPARING, SessionStateDto.ARMED -> {
                    Close
                }
            }
    }
}
