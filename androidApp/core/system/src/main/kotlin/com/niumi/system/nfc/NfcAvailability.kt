package com.niumi.system.nfc

/** Disponibilité du NFC sur l'appareil (SPEC_ANDROID §11.2, tableau des résultats UI). */
public enum class NfcAvailability {
    /** Aucun matériel NFC (`PackageManager.FEATURE_NFC` absent). */
    ABSENT,

    /** Matériel présent mais désactivé par l'utilisateur. */
    DISABLED,

    ENABLED,
}
