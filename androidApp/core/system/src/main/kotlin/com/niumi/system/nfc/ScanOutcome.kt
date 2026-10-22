package com.niumi.system.nfc

/** Résultat d'un scan NFC traité par un [NfcScanHandler] (SPEC_ANDROID §11.2). */
public sealed interface ScanOutcome {
    /** Niumi Point valide et associé : le scan a produit un effet (arrêt de la sonnerie ou plus). */
    public data object Accepted : ScanOutcome

    /**
     * Niumi Point bien formé, mais absent de la liste de la session, ou d'une version que l'app ne
     * lit pas : « Ce Niumi Point n'est pas enregistré. » (étape 38).
     */
    public data object UnknownBox : ScanOutcome

    /**
     * Tag lu, mais qui ne porte pas de Niumi Point : ni NDEF, ni URI, ou une URI que le parseur
     * commun refuse (étape 38, « Ce n'est pas un Niumi Point. »).
     */
    public data object NotANiumiPoint : ScanOutcome

    /** Lecture interrompue ou NDEF corrompu : « Ton Niumi Point n'a pas pu être lu. ». */
    public data object Unreadable : ScanOutcome

    /** Scan ignoré (par exemple un scan déjà en cours de traitement). */
    public data object Ignored : ScanOutcome
}
