package com.niumi.system.nfc

/**
 * Feuille de scan de l'écran 10 (étape 38). La lecture NFC n'est active que sur [Ready] (décision
 * Q4 du 2026-10-22) : une feuille d'échec n'écoute pas, « Réessayer » rouvre [Ready].
 */
public sealed interface ScanSheetState {
    public data object Hidden : ScanSheetState

    /** « Prêt à scanner » : la seule feuille pendant laquelle le Reader Mode est actif. */
    public data object Ready : ScanSheetState

    public data class Failed(
        val reason: ScanFailureReason,
    ) : ScanSheetState
}
