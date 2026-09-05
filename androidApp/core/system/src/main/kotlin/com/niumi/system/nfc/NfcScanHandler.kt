package com.niumi.system.nfc

/**
 * Traite une URI lue sur un tag NFC (« Interfaces transverses » du plan MVP). La seule
 * implémentation avant l'étape 18 (`PocNfcScanHandler`) vit dans `src/debug` de `:app` ;
 * `AlarmActivity` (`src/main` de `:feature:ringing`) l'obtient via `Optional<NfcScanHandler>`
 * (`@BindsOptionalOf`), absent en release.
 */
public fun interface NfcScanHandler {
    public suspend fun onUriRead(uri: String): ScanOutcome
}
