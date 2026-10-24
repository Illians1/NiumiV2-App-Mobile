package com.niumi.system.nfc

/**
 * Échec à montrer pour un résultat de scan (étape 38, SPEC_ANDROID §11.2). Le NFC coupé l'emporte
 * sur tout résultat. Sans matériel NFC ([NfcAvailability.ABSENT]), aucune feuille : l'écran le dit
 * à la place de la consigne. `null` = pas d'échec.
 */
public fun scanFailureOf(
    outcome: ScanOutcome?,
    availability: NfcAvailability,
): ScanFailureReason? =
    when (availability) {
        NfcAvailability.ABSENT -> {
            null
        }

        NfcAvailability.DISABLED -> {
            ScanFailureReason.NFC_DISABLED
        }

        NfcAvailability.ENABLED -> {
            when (outcome) {
                ScanOutcome.NotANiumiSpot -> ScanFailureReason.NOT_A_NIUMI_SPOT
                ScanOutcome.UnknownBox -> ScanFailureReason.NOT_REGISTERED
                ScanOutcome.Unreadable -> ScanFailureReason.UNREADABLE
                ScanOutcome.Accepted, ScanOutcome.Ignored, null -> null
            }
        }
    }
