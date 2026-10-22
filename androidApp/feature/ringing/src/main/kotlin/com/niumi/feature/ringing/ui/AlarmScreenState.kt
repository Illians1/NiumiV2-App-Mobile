package com.niumi.feature.ringing.ui

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanSheetState

/**
 * État d'affichage pur de l'écran 10 · Réveil (étape 38), sans dépendance à Compose ni à Android.
 * Indexé par [SessionStateDto] : l'écran reflète l'état réel du moteur, jamais une phase devinée.
 *
 * - Une seule consigne, « Rejoins ton Niumi Point », pour `RINGING`, `AWAITING_NFC` et
 *   `TRIGGERED_AWAITING_NFC` (décisions de Mehdi du 2026-10-22) : l'écran ne dit qu'une chose.
 * - `RELEASING` : le scan a eu lieu ; la date, l'heure et un cercle de chargement seuls
 *   ([showsReleaseLoader], décision du 2026-10-22), ni consigne, ni dessin, ni bouton, ni feuille.
 * - Sans matériel NFC (défensif, le manifeste l'exige) : la consigne le dit, ni bouton ni feuille.
 * - Les échecs de scan et le NFC coupé se disent dans la feuille ([sheet]), jamais à la place de
 *   la consigne.
 *
 * [readerModeRequested] : la lecture NFC n'est active que feuille « Prêt à scanner » ouverte et
 * NFC allumé (décision Q4 du 2026-10-22, parité iPhone). Aucun texte de déverrouillage : l'écran
 * n'est jamais affiché verrouillé (Lot 9).
 */
data class AlarmScreenState(
    val sessionState: SessionStateDto,
    val nfcAvailability: NfcAvailability,
    /** `null` pendant `RELEASING`, où le chargement prend la place du dessin et de la consigne. */
    val instructionText: String?,
    val showsReleaseLoader: Boolean,
    val showsScanButton: Boolean,
    val sheet: ScanSheetState,
) {
    val readerModeRequested: Boolean
        get() = sheet == ScanSheetState.Ready && nfcAvailability == NfcAvailability.ENABLED

    companion object {
        fun from(
            sessionState: SessionStateDto,
            nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
            sheet: ScanSheetState = ScanSheetState.Hidden,
        ): AlarmScreenState {
            val releasing = sessionState == SessionStateDto.RELEASING
            val scannable = !releasing && nfcAvailability != NfcAvailability.ABSENT
            return AlarmScreenState(
                sessionState = sessionState,
                nfcAvailability = nfcAvailability,
                instructionText =
                    when {
                        releasing -> null
                        nfcAvailability == NfcAvailability.ABSENT -> AlarmTexts.NFC_ABSENT
                        else -> AlarmTexts.INSTRUCTION
                    },
                showsReleaseLoader = releasing,
                showsScanButton = scannable,
                sheet = if (scannable) sheet else ScanSheetState.Hidden,
            )
        }
    }
}
