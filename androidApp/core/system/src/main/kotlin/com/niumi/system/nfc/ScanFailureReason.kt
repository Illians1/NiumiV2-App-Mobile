package com.niumi.system.nfc

/** Les quatre échecs de la feuille de scan (`SPECIFICATION.md` §14, maquettes 30 à 33). */
public enum class ScanFailureReason {
    NOT_A_NIUMI_POINT,
    NOT_REGISTERED,
    UNREADABLE,
    NFC_DISABLED,
}
