package com.niumi.feature.ringing.ui

/**
 * Les seules sorties de l'écran 10 (étape 38), groupées comme celles des écrans 1, 2 et 4. Aucune
 * n'arrête l'alarme ni ne touche la session : ouvrir, refermer ou rouvrir la feuille de scan, et
 * ouvrir les réglages NFC du téléphone. Vérifié par `AlarmScreenActionsTest`.
 */
data class AlarmScreenActions(
    /** « Scanner le Niumi Point ». */
    val openScanSheet: () -> Unit = {},
    /** « Annuler », le voile, le glissement ou le retour, feuille ouverte. */
    val cancelScan: () -> Unit = {},
    /** « Réessayer ». */
    val retryScan: () -> Unit = {},
    /** « Ouvrir les réglages », NFC coupé. */
    val openNfcSettings: () -> Unit = {},
)
