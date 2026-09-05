package com.niumi.system.nfc

/** Résultat d'un scan NFC traité par un [NfcScanHandler] (SPEC_ANDROID §11.2). */
public sealed interface ScanOutcome {
    /** Boîtier valide et associé : le scan a produit un effet (arrêt de la sonnerie ou plus). */
    public data object Accepted : ScanOutcome

    /** Payload valide, mais boîtier différent de celui associé. */
    public data object UnknownBox : ScanOutcome

    /** Tag illisible ou payload malformé. */
    public data object Unreadable : ScanOutcome

    /** Scan ignoré (par exemple un scan déjà en cours de traitement). */
    public data object Ignored : ScanOutcome
}
