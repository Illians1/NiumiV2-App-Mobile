package com.niumi.system.nfc

/**
 * Traite une URI lue sur un tag NFC (« Interfaces transverses » du plan MVP). Une seule
 * implémentation depuis l'étape 18, `HandleValidNfcUseCase`, partagée par l'écran de réveil
 * (`AlarmActivity`, `:feature:ringing`) et la feuille d'annulation (`CancelSessionViewModel`,
 * `:feature:session`, qui remplace l'écran 9 à l'étape 46) — les deux seuls endroits d'où un scan
 * peut terminer une session (§11.2).
 */
public fun interface NfcScanHandler {
    public suspend fun onUriRead(uri: String): ScanOutcome
}
