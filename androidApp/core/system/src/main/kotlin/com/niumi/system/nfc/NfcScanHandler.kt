package com.niumi.system.nfc

/**
 * Traite une URI lue sur un tag NFC (« Interfaces transverses » du plan MVP). Une seule
 * implémentation depuis l'étape 18, `HandleValidNfcUseCase`, partagée par l'écran de réveil
 * (`AlarmActivity`, `:feature:ringing`) et l'écran 9 (`ScanToModifyViewModel`,
 * `:feature:session`) — les deux seuls endroits d'où une session peut se terminer (§11.2).
 */
public fun interface NfcScanHandler {
    public suspend fun onUriRead(uri: String): ScanOutcome
}
