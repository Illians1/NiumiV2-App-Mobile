package com.niumi.feature.setup.pairing

import com.niumi.system.nfc.NfcAvailability

/**
 * État de l'écran 3 (adaptation minimale du Lot 8 : plusieurs boîtiers possibles au dépôt,
 * l'écran « Mes boîtiers » complet arrive à l'étape 31). [pairedBoxIdPrefix] reste le préfixe du
 * premier boîtier associé, assez pour que [canContinue] et l'affichage actuels restent corrects.
 */
data class PairingUiState(
    val nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
    val pairedBoxIdPrefix: String? = null,
    val message: String? = null,
    val isSessionInProgress: Boolean = false,
) {
    val isReaderModeExpected: Boolean
        get() = nfcAvailability == NfcAvailability.ENABLED && !isSessionInProgress

    val canContinue: Boolean get() = pairedBoxIdPrefix != null
}
